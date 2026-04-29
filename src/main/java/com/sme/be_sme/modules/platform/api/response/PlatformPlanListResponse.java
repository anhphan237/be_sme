package com.sme.be_sme.modules.platform.api.response;

import java.util.List;

import lombok.Data;

@Data
public class PlatformPlanListResponse {
    private List<PlanItem> items;

    @Data
    public static class PlanItem {
        private String planId;
        private String code;
        private String name;
        private Integer employeeLimitPerMonth;
        private Integer onboardingTemplateLimit;
        private Integer eventTemplateLimit;
        private Integer documentLimit;
        private Long storageLimitBytes;
        private Integer priceVndMonthly;
        private Integer priceVndYearly;
        private String status;
    }
}
