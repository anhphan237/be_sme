package com.sme.be_sme.modules.billing.api.response;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubscriptionCurrentResponse {
    private String subscriptionId;
    private String planCode;
    private String status;
    private String billingCycle;
    private Date currentPeriodStart;
    private Date currentPeriodEnd;
    private Boolean autoRenew;
}
