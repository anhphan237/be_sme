package com.sme.be_sme.modules.analytics.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlatformSubscriptionMetricsRequest {
    private String startDate;
    private String endDate;
}
