package com.sme.be_sme.modules.identity.processor;

import com.sme.be_sme.modules.identity.context.IdentityRoleAssignContext;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.identity.service.UserService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidateRoleAssignCoreProcessor extends BaseCoreProcessor<IdentityRoleAssignContext> {

    private final UserService userService;

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

        Optional<UserEntity> user = userService.findById(ctx.getBiz().getTenantId(), ctx.getRequest().getUserId());
        if (user.isEmpty() || !"ACTIVE".equalsIgnoreCase(user.get().getStatus())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "user not found or inactive");
        }
        return null;
    }
}
