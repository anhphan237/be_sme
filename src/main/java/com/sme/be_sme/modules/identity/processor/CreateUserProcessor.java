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
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

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

        BizContext safeContext = context == null ? BizContext.of(null, null, null) : context;
        String companyId = resolveCompanyId(safeContext.getTenantId(), request);

        // Check global uniqueness (DB constraint uq_users_lower_email is case-insensitive)
        userService.findByLowerEmail(request.getEmail().trim())
                .ifPresent(user -> {
                    throw AppException.of(ErrorCodes.DUPLICATED, "Email đã tồn tại trong hệ thống");
                });

        boolean hasPassword = request.getPassword() != null && !request.getPassword().isBlank();
        Date now = new Date();
        UserEntity entity = new UserEntity();
        entity.setUserId(UuidGenerator.generate());
        entity.setCompanyId(companyId);
        entity.setEmail(request.getEmail());
        entity.setPasswordHash(hasPassword ? passwordHasher.hash(request.getPassword()) : null);
        entity.setFullName(request.getFullName());
        entity.setPhone(request.getPhone());
        entity.setStatus(
                request.getStatus() != null ? request.getStatus()
                        : (hasPassword ? "ACTIVE" : "PENDING"));
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
