package com.sme.be_sme.modules.company.facade;

import com.sme.be_sme.modules.company.api.request.CompanyRegisterRequest;
import com.sme.be_sme.modules.company.api.request.CreateCompanyRequest;
import com.sme.be_sme.modules.company.api.response.CompanyRegisterResponse;
import com.sme.be_sme.modules.company.api.response.CreateCompanyResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface CompanyFacade extends OperationFacadeProvider {
    @OperationType("com.sme.company.create")
    CreateCompanyResponse createCompany(CreateCompanyRequest request);

    @OperationType("com.sme.company.register")
    CompanyRegisterResponse registerCompany(CompanyRegisterRequest request);
}
