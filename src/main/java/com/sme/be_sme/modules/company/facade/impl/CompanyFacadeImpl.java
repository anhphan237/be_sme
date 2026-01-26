package com.sme.be_sme.modules.company.facade.impl;

import com.sme.be_sme.modules.company.api.request.CompanyRegisterRequest;
import com.sme.be_sme.modules.company.api.request.CreateCompanyRequest;
import com.sme.be_sme.modules.company.api.response.CompanyRegisterResponse;
import com.sme.be_sme.modules.company.api.response.CreateCompanyResponse;
import com.sme.be_sme.modules.company.facade.CompanyFacade;
import com.sme.be_sme.modules.company.processor.CompanyRegisterProcessor;
import com.sme.be_sme.modules.company.processor.CreateCompanyProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompanyFacadeImpl extends BaseOperationFacade implements CompanyFacade {

    private final CreateCompanyProcessor createCompanyProcessor;
    private final CompanyRegisterProcessor companyRegisterProcessor;

    @Override
    public CreateCompanyResponse createCompany(CreateCompanyRequest request) {
        return call(createCompanyProcessor, request, CreateCompanyResponse.class);
    }

    @Override
    public CompanyRegisterResponse registerCompany(CompanyRegisterRequest request) {
        return call(companyRegisterProcessor, request, CompanyRegisterResponse.class);
    }
}
