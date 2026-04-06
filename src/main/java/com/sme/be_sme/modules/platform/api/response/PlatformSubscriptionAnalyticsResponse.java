package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

@Data
public class PlatformSubscriptionAnalyticsResponse {
    private Integer totalSubscriptions;
    private Integer activeSubscriptions;
    private Integer newSubscriptions;
    private Integer cancelledSubscriptions;
    private Integer suspendedSubscriptions;
    private Double churnRate;

}
