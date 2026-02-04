package com.sme.be_sme.modules.billing.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsageSummaryRequest {
    private String subscriptionId; // optional
    private String month; // optional yyyy-MM
}
