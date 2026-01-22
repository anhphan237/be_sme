package com.sme.be_sme.modules.company.facade;

import com.sme.be_sme.modules.company.api.request.CreateCompanyRequest;
import com.sme.be_sme.modules.company.api.response.CreateCompanyResponse;
import com.sme.be_sme.modules.company.processor.CreateCompanyProcessor;
import com.sme.be_sme.shared.gateway.core.BizContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompanyFacadeImpl implements CompanyFacade {

    private final CreateCompanyProcessor processor;

    @Override
    public CreateCompanyResponse createCompany(CreateCompanyRequest request) {
        return processor.process(BizContextHolder.get(), request);
    }
}
