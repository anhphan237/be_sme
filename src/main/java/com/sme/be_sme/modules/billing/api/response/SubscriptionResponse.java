package com.sme.be_sme.modules.billing.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionResponse {
    private String subscriptionId;
    private String planCode;
    private String status;
}
