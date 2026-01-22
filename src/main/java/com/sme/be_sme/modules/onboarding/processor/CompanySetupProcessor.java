package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.api.response.CreateCompanyResponse;
import com.sme.be_sme.modules.company.api.response.CreateDepartmentResponse;
import com.sme.be_sme.modules.company.processor.CreateCompanyProcessor;
import com.sme.be_sme.modules.company.processor.CreateDepartmentProcessor;
import com.sme.be_sme.modules.employee.api.request.UpsertEmployeeProfileRequest;
import com.sme.be_sme.modules.employee.processor.UpsertEmployeeProfileProcessor;
import com.sme.be_sme.modules.identity.api.request.AssignRoleRequest;
import com.sme.be_sme.modules.identity.api.response.CreateUserResponse;
import com.sme.be_sme.modules.identity.processor.AssignRoleProcessor;
import com.sme.be_sme.modules.identity.processor.CreateUserProcessor;
import com.sme.be_sme.modules.onboarding.api.request.CompanySetupRequest;
import com.sme.be_sme.modules.onboarding.api.response.CompanySetupResponse;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CompanySetupProcessor extends BaseBizProcessor<BizContext> {

    private static final String ADMIN_ROLE_CODE = "ADMIN";

    private final CreateCompanyProcessor createCompanyProcessor;
    private final CreateDepartmentProcessor createDepartmentProcessor;
    private final CreateUserProcessor createUserProcessor;

    private final UpsertEmployeeProfileProcessor upsertEmployeeProfileProcessor;
    private final AssignRoleProcessor assignRoleProcessor;

    private final ObjectMapper objectMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        CompanySetupRequest req = objectMapper.convertValue(payload, CompanySetupRequest.class);
        return process(context, req);
    }

    @Transactional
    public CompanySetupResponse process(BizContext context, CompanySetupRequest request) {
        validate(request);

        BizContext safeContext = (context == null) ? BizContext.of(null, null) : context;

        // 1) Create company (tenant)
        CreateCompanyResponse company = createCompanyProcessor.process(safeContext, request.getCompany());

        // tenant context for subsequent operations
        BizContext tenantCtx = BizContext.of(company.getCompanyId(), safeContext.getRequestId());

        // 2) Create initial department
        CreateDepartmentResponse department = createDepartmentProcessor.process(tenantCtx, request.getDepartment());

        // 3) Create admin user
        CreateUserResponse adminUser = createUserProcessor.process(tenantCtx, request.getAdminUser());

        // 4) Ensure admin employee profile exists
        upsertEmployeeProfileProcessor.process(
                tenantCtx,
                UpsertEmployeeProfileRequest.builder()
                        .userId(adminUser.getUserId())
                        .departmentId(department.getDepartmentId())
                        .employeeName(request.getAdminUser().getFullName())
                        .employeeEmail(request.getAdminUser().getEmail())
                        .employeePhone(request.getAdminUser().getPhone())
                        .status("ACTIVE")
                        .build()
        );

        // 5) Assign exactly ONE role for admin (ADMIN)
        assignRoleProcessor.process(
                tenantCtx,
                new AssignRoleRequest(adminUser.getUserId(), ADMIN_ROLE_CODE)
        );

        CompanySetupResponse res = new CompanySetupResponse();
        res.setCompanyId(company.getCompanyId());
        res.setDepartmentId(department.getDepartmentId());
        res.setAdminUserId(adminUser.getUserId());
        res.setMemberUserId(null);
        return res;
    }

    public CompanySetupResponse process(CompanySetupRequest request) {
        return process(BizContext.of(null, null), request);
    }

    private static void validate(CompanySetupRequest request) {
        if (request == null || request.getCompany() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "company is required");
        }
        if (request.getDepartment() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "department is required");
        }
        if (request.getAdminUser() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "adminUser is required");
        }
    }
}
