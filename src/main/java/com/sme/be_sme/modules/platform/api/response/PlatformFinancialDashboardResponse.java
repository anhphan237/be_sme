package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

@Data
public class PlatformFinancialDashboardResponse {
    private double mrr;
    private double totalRevenue;
    private int activeSubscriptions;
    private int newSubscriptions;
    private Double churnRate;
    private int failedPayments;
}
