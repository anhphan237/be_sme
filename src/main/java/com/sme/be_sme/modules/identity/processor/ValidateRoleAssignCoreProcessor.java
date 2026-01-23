package com.sme.be_sme.modules.identity.processor;

import com.sme.be_sme.modules.identity.context.IdentityRoleAssignContext;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidateRoleAssignCoreProcessor extends BaseCoreProcessor<IdentityRoleAssignContext> {

    private final UserMapperExt userMapperExt;

    @Override
    protected Object process(IdentityRoleAssignContext ctx) {
        if (ctx == null || ctx.getBiz() == null || ctx.getBiz().getTenantId() == null || ctx.getBiz().getTenantId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (ctx.getRequest() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (ctx.getRequest().getUserId() == null || ctx.getRequest().getUserId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "userId is required");
        }
        if (ctx.getRequest().getRoleCode() == null || ctx.getRequest().getRoleCode().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "roleCode is required");
        }

        // Optional: user must exist & ACTIVE
        int ok = userMapperExt.countActiveByCompanyIdAndUserId(ctx.getBiz().getTenantId(), ctx.getRequest().getUserId());
        if (ok == 0) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "user not found or inactive");
        }
        return null;
    }
}
