package com.sme.be_sme.modules.platform.api.response;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class PlatformSubscriptionListResponse {
    private List<SubscriptionItem> items;
    private int total;

    @Data
    public static class SubscriptionItem {
        private String subscriptionId;
        private String companyId;
        private String companyName;
        private String planCode;
        private String status;
        private String billingCycle;
        private Date currentPeriodStart;
        private Date currentPeriodEnd;
    }
}
