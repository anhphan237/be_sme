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

        int updated = userRoleMapperExt.updateRoleForUser(
                ctx.getBiz().getTenantId(),
                ctx.getRequest().getUserId(),
                ctx.getRoleId(),
                new Date()
        );

        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "assign role failed");
        }
        return null;
    }
}
