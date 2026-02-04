package com.sme.be_sme.modules.billing.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsageTrackResponse {
    private String subscriptionId;
    private String usageType;
    private Integer quantity;
}
