package com.sme.be_sme.modules.identity.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.identity.api.request.GetUserRequest;
import com.sme.be_sme.modules.identity.api.response.GetUserResponse;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.identity.service.UserService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdentityUserGetProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final EmployeeProfileMapperExt employeeProfileMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        GetUserRequest req = objectMapper.convertValue(payload, GetUserRequest.class);
        return process(context, req);
    }

    public GetUserResponse process(BizContext context, GetUserRequest request) {
        validate(context, request);

        String companyId = context.getTenantId();
        UserEntity user = userService.findById(companyId, request.getUserId())
                .orElseThrow(() -> AppException.of(ErrorCodes.NOT_FOUND, "user not found"));

        EmployeeProfileEntity profile =
                employeeProfileMapperExt.selectByCompanyIdAndUserId(companyId, request.getUserId());

        GetUserResponse response = new GetUserResponse();
        response.setUserId(user.getUserId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setStatus(user.getStatus());

        if (profile != null) {
            response.setEmployeeId(profile.getEmployeeId());
            response.setDepartmentId(profile.getDepartmentId());
            response.setEmployeeCode(profile.getEmployeeCode());
            response.setEmployeeName(profile.getEmployeeName());
            response.setEmployeeEmail(profile.getEmployeeEmail());
            response.setEmployeePhone(profile.getEmployeePhone());
            response.setJobTitle(profile.getJobTitle());
            response.setManagerUserId(profile.getManagerUserId());
            response.setStartDate(profile.getStartDate());
            response.setWorkLocation(profile.getWorkLocation());
            response.setEmployeeStatus(profile.getStatus());
        }

        return response;
    }

    private static void validate(BizContext context, GetUserRequest request) {
        if (context == null || context.getTenantId() == null || context.getTenantId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "userId is required");
        }
    }
}