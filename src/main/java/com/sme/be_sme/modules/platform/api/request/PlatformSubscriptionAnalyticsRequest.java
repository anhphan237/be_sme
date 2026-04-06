package com.sme.be_sme.modules.platform.api.request;

import lombok.Data;

@Data
public class PlatformSubscriptionAnalyticsRequest {
    private String startDate;
    private String endDate;
}
