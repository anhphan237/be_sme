package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

@Data
public class PlatformCompanyChangePlanResponse {
    private String companyId;
    private String subscriptionId;
    private String oldPlanId;
    private String newPlanId;
    private String billingCycle;
    private String message;
}
