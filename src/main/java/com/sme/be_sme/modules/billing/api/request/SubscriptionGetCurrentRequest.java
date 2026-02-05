package com.sme.be_sme.modules.billing.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionGetCurrentRequest {
    private String companyId; // optional, must match tenant if provided
}
