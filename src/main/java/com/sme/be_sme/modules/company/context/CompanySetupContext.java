package com.sme.be_sme.modules.company.context;

import com.sme.be_sme.modules.company.api.response.CreateCompanyResponse;
import com.sme.be_sme.modules.company.api.response.CreateDepartmentResponse;
import com.sme.be_sme.modules.identity.api.response.CreateUserResponse;
import com.sme.be_sme.modules.onboarding.api.request.CompanySetupRequest;
import com.sme.be_sme.modules.onboarding.api.response.CompanySetupResponse;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanySetupContext {
    private BizContext biz;
    private CompanySetupRequest request;
    private CompanySetupResponse response;

    // shared state
    private CreateCompanyResponse company;
    private CreateDepartmentResponse department;
    private CreateUserResponse adminUser;
}

