package com.sme.be_sme.modules.billing.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionUpdateRequest {
    private String subscriptionId;
    private String planCode;
    private String status;
}
