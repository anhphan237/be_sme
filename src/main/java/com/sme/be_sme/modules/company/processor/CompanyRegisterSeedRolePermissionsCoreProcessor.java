package com.sme.be_sme.modules.company.processor;

import com.sme.be_sme.modules.company.context.CompanyRegisterContext;
import com.sme.be_sme.modules.identity.infrastructure.mapper.PermissionMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.RolePermissionMapper;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.RoleEntity;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.RolePermissionEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class CompanyRegisterSeedRolePermissionsCoreProcessor extends BaseCoreProcessor<CompanyRegisterContext> {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_HR = "HR";
    private static final String ROLE_MANAGER = "MANAGER";

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    /**
     * NOTE theo permission catalog:
     * - Không có *.list cho department/role => tạm dùng *.read
     * - Assign role cho user dùng: com.sme.identity.role.assign
     */
    private static final List<String> HR_PERMISSION_CODES = List.of(
            // Department
            "com.sme.company.department.create",
            "com.sme.company.department.read",
            "com.sme.company.department.update",
            "com.sme.company.department.assignUser",

            // User
            "com.sme.identity.user.create",
            "com.sme.identity.user.read",
            "com.sme.identity.user.list",
            "com.sme.identity.user.update",
            "com.sme.identity.user.disable",

            // Role/RBAC (assign role)
            "com.sme.identity.role.read",
            "com.sme.identity.role.assign"
    );

    private static final List<String> MANAGER_PERMISSION_CODES = List.of(
            "com.sme.identity.user.list",
            "com.sme.identity.user.read",
            "com.sme.company.department.read"
    );

    @Override
    protected Object process(CompanyRegisterContext ctx) {
        String companyId = requireCompanyId(ctx);

        Map<String, String> roleIdByCode = indexRoleIds(ctx.getDefaultRoles());
        String adminRoleId = mustRole(roleIdByCode, ROLE_ADMIN);
        String hrRoleId = mustRole(roleIdByCode, ROLE_HR);
        String managerRoleId = mustRole(roleIdByCode, ROLE_MANAGER);

        // ADMIN -> all global active permissions
        List<String> allPermIds = permissionMapper.selectAllGlobalActivePermissionIds();
        if (allPermIds == null || allPermIds.isEmpty()) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "global permissions empty");
        }

        List<String> hrPermIds = resolvePermIdsByCodesOrThrow(HR_PERMISSION_CODES, "HR");
        List<String> managerPermIds = resolvePermIdsByCodesOrThrow(MANAGER_PERMISSION_CODES, "MANAGER");

        List<RolePermissionEntity> rows = new ArrayList<>(allPermIds.size() + hrPermIds.size() + managerPermIds.size());
        rows.addAll(buildRows(companyId, adminRoleId, allPermIds));
        rows.addAll(buildRows(companyId, hrRoleId, hrPermIds));
        rows.addAll(buildRows(companyId, managerRoleId, managerPermIds));

        int inserted = rolePermissionMapper.insertBatch(rows);
        if (inserted != rows.size()) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "seed role_permissions failed");
        }
        return null;
    }

    private List<String> resolvePermIdsByCodesOrThrow(List<String> codes, String roleCode) {
        if (codes == null || codes.isEmpty()) return List.of();

        List<String> ids = permissionMapper.selectGlobalActivePermissionIdsByCodes(codes);
        if (ids == null) ids = List.of();

        if (ids.size() != codes.size()) {
            List<String> foundCodes = permissionMapper.selectGlobalActivePermissionCodesByCodes(codes);
            Set<String> found = foundCodes == null ? Set.of() : new HashSet<>(foundCodes);

            List<String> missing = new ArrayList<>();
            for (String c : codes) if (!found.contains(c)) missing.add(c);

            throw AppException.of(ErrorCodes.INTERNAL_ERROR,
                    "missing permissions for role " + roleCode + ": " + missing);
        }
        return ids;
    }

    private static List<RolePermissionEntity> buildRows(String companyId, String roleId, List<String> permIds) {
        List<RolePermissionEntity> list = new ArrayList<>(permIds.size());
        for (String pid : permIds) {
            RolePermissionEntity e = new RolePermissionEntity();
            e.setRolePermissionId(UuidGenerator.generate());
            e.setCompanyId(companyId);
            e.setRoleId(roleId);
            e.setPermissionId(pid);
            list.add(e);
        }
        return list;
    }

    private static String requireCompanyId(CompanyRegisterContext ctx) {
        if (ctx.getCompany() == null || ctx.getCompany().getCompanyId() == null || ctx.getCompany().getCompanyId().isBlank()) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "companyId missing");
        }
        return ctx.getCompany().getCompanyId();
    }

    private static Map<String, String> indexRoleIds(List<RoleEntity> roles) {
        if (roles == null) return Map.of();
        Map<String, String> m = new HashMap<>();
        for (RoleEntity r : roles) {
            if (r != null && r.getCode() != null && r.getRoleId() != null) {
                m.put(r.getCode(), r.getRoleId());
            }
        }
        return m;
    }

    private static String mustRole(Map<String, String> roleIdByCode, String code) {
        String id = roleIdByCode.get(code);
        if (id == null || id.isBlank()) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "role not found: " + code);
        }
        return id;
    }
}
