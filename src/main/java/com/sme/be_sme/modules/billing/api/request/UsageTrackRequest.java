package com.sme.be_sme.modules.billing.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsageTrackRequest {
    private String subscriptionId;
    private String usageType;
    private Integer quantity;
}
