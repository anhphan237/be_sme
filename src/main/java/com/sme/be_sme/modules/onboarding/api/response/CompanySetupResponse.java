package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanySetupResponse {
    private String companyId;
    private String departmentId;
    private String adminUserId;
    private String memberUserId;
}
