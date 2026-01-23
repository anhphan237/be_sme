package com.sme.be_sme.modules.identity.processor;

import com.sme.be_sme.modules.identity.api.request.AssignRoleRequest;
import com.sme.be_sme.modules.identity.api.response.AssignRoleResponse;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserRoleEntity;
import com.sme.be_sme.modules.identity.infrastructure.mapper.RoleMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserRoleMapperExt;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class AssignRoleProcessor {

    private final RoleMapperExt roleMapperExt;
    private final UserRoleMapperExt userRoleMapperExt;

    public AssignRoleResponse process(BizContext ctx, AssignRoleRequest req) {
        if (ctx == null || ctx.getTenantId() == null || ctx.getTenantId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (req == null || req.getUserId() == null || req.getUserId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "userId is required");
        }
        if (req.getRoleCode() == null || req.getRoleCode().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "roleCode is required");
        }

        String companyId = ctx.getTenantId();

        // 1) resolve roleId by roleCode
        String roleId = roleMapperExt.selectRoleIdByCode(companyId, req.getRoleCode());
        if (roleId == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "roleCode not found: " + req.getRoleCode());
        }

        // 2) idempotent insert
        int exists = userRoleMapperExt.countByCompanyIdAndUserIdAndRoleId(companyId, req.getUserId(), roleId);
        AssignRoleResponse res = new AssignRoleResponse();
        res.setUserId(req.getUserId());
        res.setRoleId(roleId);
        res.setRoleCode(req.getRoleCode());

        if (exists > 0) {
            res.setStatus("EXISTS");   // idempotent
            return res;
        }

        UserRoleEntity e = new UserRoleEntity();
        e.setUserRoleId(UuidGenerator.generate());
        e.setCompanyId(companyId);
        e.setUserId(req.getUserId());
        e.setRoleId(roleId);
        e.setCreatedAt(new Date());

        userRoleMapperExt.insert(e);

        res.setStatus("ASSIGNED");
        return res;
    }
}
