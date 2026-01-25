package com.sme.be_sme.modules.analytics.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyOnboardingSummaryResponse {
    private String companyId;
    private int totalEmployees;
    private int completedCount;
}
