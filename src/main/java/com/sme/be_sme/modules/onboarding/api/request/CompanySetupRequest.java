package com.sme.be_sme.modules.onboarding.api.request;

import com.sme.be_sme.modules.company.api.request.CreateCompanyRequest;
import com.sme.be_sme.modules.company.api.request.CreateDepartmentRequest;
import com.sme.be_sme.modules.identity.api.request.CreateUserRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanySetupRequest {
    private CreateCompanyRequest company;
    private CreateDepartmentRequest department;
    private CreateUserRequest adminUser;
    private CreateUserRequest memberUser;
}
