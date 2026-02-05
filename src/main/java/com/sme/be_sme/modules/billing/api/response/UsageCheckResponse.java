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
}
