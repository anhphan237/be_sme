package com.sme.be_sme.modules.identity.invite;

import com.sme.be_sme.modules.identity.invite.infrastructure.entity.UserInviteTokenEntity;
import com.sme.be_sme.modules.identity.invite.infrastructure.mapper.UserInviteTokenMapper;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

/**
 * Manages invite tokens for secure set-password flow.
 */
@Service
@RequiredArgsConstructor
public class InviteTokenService {

    private static final int TOKEN_BYTES = 32;

    private final UserInviteTokenMapper inviteTokenMapper;
    private final InviteProperties inviteProperties;

    /**
     * Creates a new invite token for the user. Returns raw token (to put in email link).
     * Caller must not log or persist the raw token.
     */
    public String createToken(String userId) {
        String rawToken = generateSecureToken();
        String tokenHash = sha256(rawToken);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, inviteProperties.getTokenTtlHours());
        Date expiresAt = cal.getTime();

        UserInviteTokenEntity entity = new UserInviteTokenEntity();
        entity.setInviteTokenId(UuidGenerator.generate());
        entity.setUserId(userId);
        entity.setTokenHash(tokenHash);
        entity.setExpiresAt(expiresAt);
        entity.setCreatedAt(new Date());
        entity.setUsedAt(null);

        inviteTokenMapper.insert(entity);
        return rawToken;
    }

    /**
     * Validates token and returns userId if valid. Throws if expired or already used.
     */
    public String validateAndGetUserId(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "Token is required");
        }
        String tokenHash = sha256(rawToken);
        UserInviteTokenEntity entity = inviteTokenMapper.selectByTokenHash(tokenHash);
        if (entity == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "Invalid or expired token");
        }
        if (entity.getUsedAt() != null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "Token already used");
        }
        if (entity.getExpiresAt().before(new Date())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "Token expired");
        }
        return entity.getUserId();
    }

    /**
     * Marks token as used. Call after successfully setting password.
     */
    public void markTokenUsed(String rawToken) {
        String tokenHash = sha256(rawToken);
        UserInviteTokenEntity entity = inviteTokenMapper.selectByTokenHash(tokenHash);
        if (entity != null && entity.getUsedAt() == null) {
            inviteTokenMapper.markUsed(entity.getInviteTokenId(), new Date());
        }
    }

    public String buildSetPasswordUrl(String rawToken) {
        String base = inviteProperties.getBaseUrl().replaceAll("/$", "");
        return base + "/invite/set-password?token=" + rawToken;
    }

    private String generateSecureToken() {
        SecureRandom sr = new SecureRandom();
        byte[] bytes = new byte[TOKEN_BYTES];
        sr.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
