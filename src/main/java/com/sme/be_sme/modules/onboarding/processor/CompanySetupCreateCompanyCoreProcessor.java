package com.sme.be_sme.modules.onboarding.processor;

import com.sme.be_sme.modules.company.api.response.CreateCompanyResponse;
import com.sme.be_sme.modules.company.context.CompanySetupContext;
import com.sme.be_sme.modules.company.processor.CreateCompanyProcessor;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompanySetupCreateCompanyCoreProcessor extends BaseCoreProcessor<CompanySetupContext> {

    private final CreateCompanyProcessor createCompanyProcessor;

    @Override
    protected Object process(CompanySetupContext ctx) {
        BizContext safe = (ctx.getBiz() == null) ? BizContext.of(null, null) : ctx.getBiz();
        CreateCompanyResponse company = createCompanyProcessor.process(safe, ctx.getRequest().getCompany());
        ctx.setCompany(company);
        return null;
    }
}

