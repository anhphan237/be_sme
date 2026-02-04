package com.sme.be_sme.modules.analytics.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyOnboardingByDepartmentRequest {
    private String companyId;
    private String startDate;
    private String endDate;
}
