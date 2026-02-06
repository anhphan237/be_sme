package com.sme.be_sme.modules.analytics.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlatformSubscriptionMetricsResponse {
    private int activeSubscriptions;
    private double monthlyRecurringRevenue;
    /** Active subscriptions at start of period (for churn denominator) */
    private int activeAtStart;
    /** Subscriptions that churned (ACTIVE -> cancelled/suspended) in the period */
    private int churnedCount;
    /** Churn rate: churnedCount / max(1, activeAtStart) */
    private Double churnRate;
}
