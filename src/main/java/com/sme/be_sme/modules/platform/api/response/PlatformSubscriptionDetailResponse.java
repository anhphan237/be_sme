package com.sme.be_sme.modules.platform.api.response;

import java.util.Date;

import lombok.Data;

@Data
public class PlatformSubscriptionDetailResponse {
    private String subscriptionId;
    private String companyId;
    private String companyName;
    private String planCode;
    private String planName;
    private String status;
    private String billingCycle;
    private Date currentPeriodStart;
    private Date currentPeriodEnd;
    private Boolean autoRenew;
}
