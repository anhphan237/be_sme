package com.sme.be_sme.modules.analytics.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlatformSubscriptionMetricsResponse {
    private int activeSubscriptions;
    private double monthlyRecurringRevenue;
}
