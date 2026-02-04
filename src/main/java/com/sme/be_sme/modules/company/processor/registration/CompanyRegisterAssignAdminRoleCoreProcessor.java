package com.sme.be_sme.modules.company.processor.registration;

import com.sme.be_sme.modules.company.context.CompanyRegisterContext;
import com.sme.be_sme.modules.identity.infrastructure.mapper.RoleMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserRoleMapper;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserRoleEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import com.sme.be_sme.shared.util.UuidGenerator;

@Component
@RequiredArgsConstructor
public class CompanyRegisterAssignAdminRoleCoreProcessor extends BaseCoreProcessor<CompanyRegisterContext> {

    private static final String ADMIN_ROLE_CODE = "ADMIN";

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    protected Object process(CompanyRegisterContext ctx) {
        if (ctx.getCompany() == null || ctx.getCompany().getCompanyId() == null) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "companyId missing after createCompany");
        }
        if (ctx.getAdminUser() == null || ctx.getAdminUser().getUserId() == null) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "adminUserId missing after createAdminUser");
        }

        String companyId = ctx.getCompany().getCompanyId();
        String roleId = resolveAdminRoleId(companyId);

        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUserRoleId(UuidGenerator.generate());
        userRole.setCompanyId(companyId);
        userRole.setUserId(ctx.getAdminUser().getUserId());
        userRole.setRoleId(roleId);
        userRole.setCreatedAt(new Date());

        int inserted = userRoleMapper.insert(userRole);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "assign admin role failed");
        }
        return null;
    }

    private String resolveAdminRoleId(String companyId) {
        String roleId = roleMapper.selectByCompanyIdAndRoleCode(companyId, ADMIN_ROLE_CODE);
        if (roleId == null) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "admin role not found");
        }
        return roleId;
    }
}
