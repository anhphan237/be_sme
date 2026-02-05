package com.sme.be_sme.modules.billing.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanGetRequest {
    /** Optional. If not provided, plan is resolved from tenant's current subscription. */
    private String planId;
}
