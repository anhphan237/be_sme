package com.sme.be_sme.modules.identity.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.api.request.LoginRequest;
import com.sme.be_sme.modules.identity.api.response.LoginResponse;
import com.sme.be_sme.modules.identity.api.response.LoginUserInfo;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.identity.infrastructure.repository.UserRoleRepository;
import com.sme.be_sme.modules.identity.service.UserService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.security.JwtProperties;
import com.sme.be_sme.shared.security.JwtService;
import com.sme.be_sme.shared.security.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class IdentityLoginProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final UserRoleRepository userRoleRepository;
    private final PasswordHasher passwordHasher;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    protected Object doProcess(BizContext context, JsonNode payload) {
        LoginRequest request = objectMapper.convertValue(payload, LoginRequest.class);
        validate(context, request);
        return process(context, request);
    }

    private LoginResponse process(BizContext context, LoginRequest request) {
        String email = request.getEmail().trim();

        UserEntity user = userService.findByEmail(email)
                .orElseThrow(() -> AppException.of(ErrorCodes.UNAUTHORIZED, "invalid credentials"));

        String companyId = user.getCompanyId();

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "user is inactive");
        }

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw AppException.of(ErrorCodes.UNAUTHORIZED, "invalid credentials");
        }

        if (!passwordHasher.matches(request.getPassword(), user.getPasswordHash())) {
            throw AppException.of(ErrorCodes.UNAUTHORIZED, "invalid credentials");
        }

        Set<String> roles = userRoleRepository.findRoles(companyId, user.getUserId());
        String token = jwtService.issueAccessToken(user.getUserId(), companyId, roles);

        user.setLastLoginAt(new Date());
        user.setUpdatedAt(new Date());
        userService.updateUser(user);

        LoginUserInfo userInfo = new LoginUserInfo();
        userInfo.setId(user.getUserId());
        userInfo.setFullName(user.getFullName());
        userInfo.setEmail(user.getEmail());
        userInfo.setRoleCode(roles.isEmpty() ? null : roles.iterator().next());
        userInfo.setTenantId(companyId);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        response.setTokenType("Bearer");
        response.setExpiresInSeconds(jwtProperties.getAccessTtlSeconds());
        response.setUser(userInfo);
        return response;
    }

    private static void validate(BizContext context, LoginRequest request) {
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "email is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "password is required");
        }
    }
}
