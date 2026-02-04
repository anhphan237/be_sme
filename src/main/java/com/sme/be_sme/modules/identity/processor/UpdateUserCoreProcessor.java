package com.sme.be_sme.modules.identity.processor;

import com.sme.be_sme.modules.identity.api.request.UpdateUserRequest;
import com.sme.be_sme.modules.identity.context.IdentityUpdateUserContext;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.identity.service.UserService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.security.PasswordHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateUserCoreProcessor {

    private final UserService userService;
    private final PasswordHasher passwordHasher;

    public void process(IdentityUpdateUserContext context, UpdateUserRequest request) {
        String companyId = context.getTenantId();

        log.info("Updating user with company id {}", companyId);
        log.info("Updating user with user id {}", request.getUserId());

        UserEntity existing = userService.findById(companyId, request.getUserId())
                .orElseThrow(() -> AppException.of(ErrorCodes.NOT_FOUND, "user not found"));

        if (request.getEmail() != null) existing.setEmail(request.getEmail());
        if (request.getFullName() != null) existing.setFullName(request.getFullName());
        if (request.getPhone() != null) existing.setPhone(request.getPhone());
        if (request.getStatus() != null) existing.setStatus(request.getStatus());

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            existing.setPasswordHash(passwordHasher.hash(request.getNewPassword()));
        }

        existing.setUpdatedAt(new Date());

        context.setUserId(existing.getUserId());
        context.setUserEmail(existing.getEmail());
        context.setFullName(existing.getFullName());
        context.setUserStatus(existing.getStatus());

        userService.updateUser(existing);
    }
}
