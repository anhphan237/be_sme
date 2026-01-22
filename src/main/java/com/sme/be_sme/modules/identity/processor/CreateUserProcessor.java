package com.sme.be_sme.modules.identity.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.api.request.CreateUserRequest;
import com.sme.be_sme.modules.identity.api.response.CreateUserResponse;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.identity.service.UserService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.security.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateUserProcessor extends BaseBizProcessor<BizContext> {

    private final UserService userService;
    private final PasswordHasher passwordHasher;
    private final ObjectMapper objectMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        CreateUserRequest req = objectMapper.convertValue(payload, CreateUserRequest.class);
        return process(context, req);
    }

    public CreateUserResponse process(BizContext context, CreateUserRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "email is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "password is required");
        }

        BizContext safeContext = context == null ? BizContext.of(null, null) : context;
        String companyId = resolveCompanyId(safeContext.getTenantId(), request);

        userService.findByEmail(companyId, request.getEmail())
                .ifPresent(user -> {
                    throw AppException.of(ErrorCodes.BAD_REQUEST, "email already exists");
                });

        Date now = new Date();
        UserEntity entity = new UserEntity();
        entity.setUserId(UUID.randomUUID().toString());
        entity.setCompanyId(companyId);
        entity.setEmail(request.getEmail());
        entity.setPasswordHash(passwordHasher.hash(request.getPassword()));
        entity.setFullName(request.getFullName());
        entity.setPhone(request.getPhone());
        entity.setStatus(request.getStatus() == null ? "ACTIVE" : request.getStatus());
        entity.setLastLoginAt(null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        userService.createUser(entity);

        CreateUserResponse res = new CreateUserResponse();
        res.setUserId(entity.getUserId());
        res.setEmail(entity.getEmail());
        res.setFullName(entity.getFullName());
        res.setStatus(entity.getStatus());
        return res;
    }

    public CreateUserResponse process(String tenantId, CreateUserRequest request) {
        return process(BizContext.of(tenantId, null), request);
    }

    private String resolveCompanyId(String tenantId, CreateUserRequest request) {
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId;
        }
        if (request.getCompanyId() != null && !request.getCompanyId().isBlank()) {
            return request.getCompanyId();
        }
        throw AppException.of(ErrorCodes.BAD_REQUEST, "companyId is required");
    }
}
