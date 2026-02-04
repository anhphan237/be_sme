package com.sme.be_sme.modules.analytics.api.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyOnboardingByDepartmentResponse {
    private String companyId;
    private List<DepartmentOnboardingStatsResponse> departments;
}
