package com.sme.be_sme.modules.identity.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.employee.api.request.UpsertEmployeeProfileRequest;
import com.sme.be_sme.modules.employee.api.response.UpsertEmployeeProfileResponse;
import com.sme.be_sme.modules.employee.processor.UpsertEmployeeProfileProcessor;
import com.sme.be_sme.modules.identity.api.request.AssignRoleRequest;
import com.sme.be_sme.modules.identity.api.request.CreateUserRequest;
import com.sme.be_sme.modules.identity.api.response.CreateUserResponse;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gateway operation for: com.sme.identity.user.create
 *
 * Responsibilities (workflow 1):
 *  - create users
 *  - create/update employee_profiles
 *  - assign exactly one role (user_roles) via roleCode -> roleId
 */
@Component
@RequiredArgsConstructor
public class IdentityUserCreateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final CreateUserProcessor createUserProcessor;
    private final UpsertEmployeeProfileProcessor upsertEmployeeProfileProcessor;
    private final AssignRoleProcessor assignRoleProcessor;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        CreateUserRequest req = objectMapper.convertValue(payload, CreateUserRequest.class);
        return process(context, req);
    }

    @Transactional
    public CreateUserResponse process(BizContext context, CreateUserRequest request) {
        validate(context, request);

        CreateUserResponse user = createUserProcessor.process(context, request);

        UpsertEmployeeProfileResponse profile = upsertEmployeeProfileProcessor.process(
                context,
                UpsertEmployeeProfileRequest.builder()
                        .userId(user.getUserId())
                        .departmentId(request.getDepartmentId())
                        .employeeCode(request.getEmployeeCode())
                        .employeeName(request.getFullName())
                        .employeeEmail(request.getEmail())
                        .employeePhone(request.getPhone())
                        .jobTitle(request.getJobTitle())
                        .managerUserId(request.getManagerUserId())
                        .startDate(request.getStartDate())
                        .workLocation(request.getWorkLocation())
                        .status(request.getStatus() == null ? "ACTIVE" : request.getStatus())
                        .build()
        );

        assignRoleProcessor.process(context, new AssignRoleRequest(user.getUserId(), request.getRoleCode()));

        // ✅ enrich response for FE
        user.setEmployeeId(profile.getEmployeeId());
        // optional echo
        user.setRoleCode(request.getRoleCode());

        return user;
    }

    private static void validate(BizContext context, CreateUserRequest request) {
        if (context == null || context.getTenantId() == null || context.getTenantId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        if (isBlank(request.getEmail())) throw AppException.of(ErrorCodes.BAD_REQUEST, "email is required");
        if (isBlank(request.getPassword())) throw AppException.of(ErrorCodes.BAD_REQUEST, "password is required");
        if (isBlank(request.getFullName())) throw AppException.of(ErrorCodes.BAD_REQUEST, "fullName is required");
        if (isBlank(request.getDepartmentId())) throw AppException.of(ErrorCodes.BAD_REQUEST, "departmentId is required");
        if (isBlank(request.getRoleCode())) throw AppException.of(ErrorCodes.BAD_REQUEST, "roleCode is required");
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}

