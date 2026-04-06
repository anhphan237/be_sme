package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

import java.util.List;

@Data
public class PlatformRevenueAnalyticsResponse {
    private Double mrr;
    private Double arr;
    private Double totalRevenue;
    private List<RevenueByPlanItem> revenueByPlans;

    @Data
    public static class RevenueByPlanItem {
        private String planId;
        private String planCode;
        private String planName;
        private Double revenue;
        private Integer subscriptionCount;
    }
}