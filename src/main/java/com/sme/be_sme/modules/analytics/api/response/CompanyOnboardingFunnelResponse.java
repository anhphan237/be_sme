package com.sme.be_sme.modules.analytics.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyOnboardingFunnelResponse {
    private String companyId;
    private int totalInstances;
    private int activeCount;
    private int completedCount;
    private int cancelledCount;
    private int otherCount;
}
