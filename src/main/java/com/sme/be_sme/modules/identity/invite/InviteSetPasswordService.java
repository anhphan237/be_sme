package com.sme.be_sme.modules.identity.invite;

import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.identity.service.UserService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.security.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class InviteSetPasswordService {

    private final InviteTokenService inviteTokenService;
    private final UserService userService;
    private final PasswordHasher passwordHasher;

    @Transactional
    public void setPassword(String token, String newPassword) {
        String userId = inviteTokenService.validateAndGetUserId(token);

        UserEntity user = userService.findByUserId(userId)
                .orElseThrow(() -> AppException.of(ErrorCodes.NOT_FOUND, "User not found"));

        if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "Password already set");
        }

        user.setPasswordHash(passwordHasher.hash(newPassword));
        user.setStatus("ACTIVE");
        user.setUpdatedAt(new Date());
        userService.updateUser(user);

        inviteTokenService.markTokenUsed(token);
    }
}
