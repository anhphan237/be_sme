package com.sme.be_sme.modules.billing.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DunningRetryRequest {
    /** Dunning case to retry (optional if subscriptionId provided) */
    private String dunningCaseId;
    /** Subscription to retry; used to find dunning case if dunningCaseId not set */
    private String subscriptionId;
}
