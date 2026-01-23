package com.sme.be_sme.modules.identity.processor;

import com.sme.be_sme.modules.identity.context.IdentityUserDisableContext;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.identity.service.UserService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class DisableUserCoreProcessor extends BaseCoreProcessor<IdentityUserDisableContext> {

    private final UserService userService;

    @Override
    protected Object process(IdentityUserDisableContext ctx) {
        String companyId = ctx.getBiz().getTenantId();
        String userId = ctx.getRequest().getUserId();

        UserEntity user = userService.findById(companyId, userId)
                .orElseThrow(() -> AppException.of(ErrorCodes.NOT_FOUND, "user not found"));

        user.setStatus(ctx.getStatus());
        user.setUpdatedAt(new Date());
        userService.updateUser(user);

        return null;
    }
}

