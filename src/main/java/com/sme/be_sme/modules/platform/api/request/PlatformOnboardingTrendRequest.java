package com.sme.be_sme.modules.platform.api.request;

import lombok.Data;

@Data
public class PlatformOnboardingTrendRequest {
    private String startDate;
    private String endDate;
    private String groupBy;
    private Boolean comparePrevious;
}
