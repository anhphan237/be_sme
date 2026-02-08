package com.sme.be_sme.modules.billing.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsageCheckResponse {
    /** Number of onboarding instances created by the tenant in the given month */
    private int currentUsage;
    /** yyyy-MM */
    private String month;
    /** Plan limit for the current subscription (null if no plan) */
    private Integer employeeLimitPerMonth;
    /** NONE | APPROACHING (>=80%) | EXCEEDED (>=100%) */
    private String alertLevel;
    /** Usage as percent of limit (0-100+), null if no limit */
    private Integer limitPercent;
}
