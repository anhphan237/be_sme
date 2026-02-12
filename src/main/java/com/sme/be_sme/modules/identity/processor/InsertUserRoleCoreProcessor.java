package com.sme.be_sme.modules.identity.processor;

import com.sme.be_sme.modules.identity.context.IdentityRoleAssignContext;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserRoleMapperExt;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class InsertUserRoleCoreProcessor extends BaseCoreProcessor<IdentityRoleAssignContext> {

    private final UserRoleMapperExt userRoleMapperExt;

    @Override
    protected Object process(IdentityRoleAssignContext ctx) {
        String companyId = ctx.getBiz().getTenantId();
        String userId = ctx.getRequest().getUserId();
        String newRoleId = ctx.getRoleId();

        String oldRoleId = userRoleMapperExt.selectOneRoleIdByCompanyAndUser(companyId, userId);
        if (oldRoleId == null || oldRoleId.isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "user has no role to update");
        }

        int updated = userRoleMapperExt.updateRoleForUser(
                companyId,
                userId,
                oldRoleId,
                newRoleId,
                new Date()
        );

        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "assign role failed");
        }
        return null;
    }
}
