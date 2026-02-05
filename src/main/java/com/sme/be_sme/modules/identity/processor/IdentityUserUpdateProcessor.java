package com.sme.be_sme.modules.identity.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.employee.api.request.UpsertEmployeeProfileRequest;
import com.sme.be_sme.modules.employee.api.response.UpsertEmployeeProfileResponse;
import com.sme.be_sme.modules.employee.processor.UpsertEmployeeProfileProcessor;
import com.sme.be_sme.modules.identity.api.request.UpdateUserRequest;
import com.sme.be_sme.modules.identity.api.response.UpdateUserResponse;
import com.sme.be_sme.modules.identity.context.IdentityUpdateUserContext;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class IdentityUserUpdateProcessor extends BaseBizProcessor<IdentityUpdateUserContext> {

    private final ObjectMapper objectMapper;

    private final UpdateUserCoreProcessor updateUserCoreProcessor; // update users
    private final UpsertEmployeeProfileProcessor upsertEmployeeProfileProcessor;

    @Override
    public Object doProcess(IdentityUpdateUserContext context, JsonNode payload) {
        UpdateUserRequest req = objectMapper.convertValue(payload, UpdateUserRequest.class);
        return process(context, req);
    }

    @Transactional
    public UpdateUserResponse process(IdentityUpdateUserContext context, UpdateUserRequest request) {
        validate(context, request);

        // 1) update users
        updateUserCoreProcessor.process(context, request);

        // 2) upsert employee_profiles by (company_id, user_id)
        UpsertEmployeeProfileResponse profile = upsertEmployeeProfileProcessor.process(
                context,
                UpsertEmployeeProfileRequest.builder()
                        .userId(request.getUserId())
                        .departmentId(request.getDepartmentId())
                        .employeeCode(request.getEmployeeCode())
                        .employeeName(
                                request.getEmployeeName() != null ? request.getEmployeeName() : request.getFullName()
                        )
                        .employeeEmail(
                                request.getEmployeeEmail() != null ? request.getEmployeeEmail() : request.getEmail()
                        )
                        .employeePhone(
                                request.getEmployeePhone() != null ? request.getEmployeePhone() : request.getPhone()
                        )
                        .jobTitle(request.getJobTitle())
                        .managerUserId(request.getManagerUserId())
                        .startDate(request.getStartDate())
                        .workLocation(request.getWorkLocation())
                        .status(request.getStatus())
                        .build()
        );

        UpdateUserResponse res = new UpdateUserResponse();
        res.setUserId(request.getUserId());
        res.setEmployeeId(profile.getEmployeeId());
        res.setStatus(context.getUserStatus());
        return res;
    }

    private static void validate(BizContext context, UpdateUserRequest request) {
        if (context == null || context.getTenantId() == null || context.getTenantId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "userId is required");
        }
    }
}
