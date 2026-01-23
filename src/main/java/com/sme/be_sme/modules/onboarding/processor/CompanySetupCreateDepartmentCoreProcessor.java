package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.api.request.CreateDepartmentRequest;
import com.sme.be_sme.modules.company.api.response.CreateDepartmentResponse;
import com.sme.be_sme.modules.company.context.CompanySetupContext;
import com.sme.be_sme.modules.company.processor.CreateDepartmentProcessor;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompanySetupCreateDepartmentCoreProcessor extends BaseCoreProcessor<CompanySetupContext> {

    private final CreateDepartmentProcessor createDepartmentProcessor;
    private final ObjectMapper objectMapper;

    @Override
    protected Object process(CompanySetupContext ctx) {
        String companyId = ctx.getCompany().getCompanyId();
        String requestId = ctx.getBiz() != null ? ctx.getBiz().getRequestId() : null;

        CreateDepartmentRequest depReq = ctx.getRequest().getDepartment();
        depReq.setCompanyId(companyId); // enforce tenant

        JsonNode payload = objectMapper.valueToTree(depReq);

        CreateDepartmentResponse department = (CreateDepartmentResponse)
                createDepartmentProcessor.execute(companyId, requestId, payload);

        ctx.setDepartment(department);
        return null;
    }
}

