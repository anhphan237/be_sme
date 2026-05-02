package com.sme.be_sme.modules.analytics.api.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyOnboardingTrendResponse {
    private String companyId;
    /** Echo of resolved granularity: DAY, MONTH, or YEAR. */
    private String granularity;
    private List<CompanyOnboardingTrendPoint> points;
}
