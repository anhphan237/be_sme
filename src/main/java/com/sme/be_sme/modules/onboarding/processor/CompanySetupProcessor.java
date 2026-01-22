package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.api.response.CreateCompanyResponse;
import com.sme.be_sme.modules.company.api.response.CreateDepartmentResponse;
import com.sme.be_sme.modules.company.processor.CreateCompanyProcessor;
import com.sme.be_sme.modules.company.processor.CreateDepartmentProcessor;
import com.sme.be_sme.modules.identity.api.response.CreateUserResponse;
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

    private final CreateCompanyProcessor createCompanyProcessor;
    private final CreateDepartmentProcessor createDepartmentProcessor;
    private final CreateUserProcessor createUserProcessor;
    private final ObjectMapper objectMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        CompanySetupRequest req = objectMapper.convertValue(payload, CompanySetupRequest.class);
        return process(context, req);
    }

    @Transactional
    public CompanySetupResponse process(BizContext context, CompanySetupRequest request) {
        if (request == null || request.getCompany() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "company is required");
        }
        if (request.getDepartment() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "department is required");
        }
        if (request.getAdminUser() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "adminUser is required");
        }

        BizContext safeContext = context == null ? BizContext.of(null, null) : context;
        CreateCompanyResponse company = createCompanyProcessor.process(safeContext, request.getCompany());
        CreateDepartmentResponse department = createDepartmentProcessor.process(
                BizContext.of(company.getCompanyId(), safeContext.getRequestId()),
                request.getDepartment()
        );

        CreateUserResponse adminUser = createUserProcessor.process(
                BizContext.of(company.getCompanyId(), safeContext.getRequestId()),
                request.getAdminUser()
        );

        CreateUserResponse memberUser = null;
        if (request.getMemberUser() != null) {
            memberUser = createUserProcessor.process(
                    BizContext.of(company.getCompanyId(), safeContext.getRequestId()),
                    request.getMemberUser()
            );
        }

        CompanySetupResponse res = new CompanySetupResponse();
        res.setCompanyId(company.getCompanyId());
        res.setDepartmentId(department.getDepartmentId());
        res.setAdminUserId(adminUser.getUserId());
        if (memberUser != null) {
            res.setMemberUserId(memberUser.getUserId());
        }
        return res;
    }

    public CompanySetupResponse process(CompanySetupRequest request) {
        return process(BizContext.of(null, null), request);
    }
}
