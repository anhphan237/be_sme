package com.sme.be_sme.modules.platform.api.request;

import lombok.Data;

@Data
public class PlatformCompanyChangePlanRequest {
    private String companyId;
    private String subscriptionId;
    private String newPlanId;
    private String billingCycle;
    private String note;
}
