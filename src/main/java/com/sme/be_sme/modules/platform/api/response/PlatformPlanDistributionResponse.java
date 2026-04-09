package com.sme.be_sme.modules.platform.api.response;

import java.util.List;
import lombok.Data;

@Data
public class PlatformPlanDistributionResponse {
    private Integer totalCompanies;
    private Integer totalEmployees;
    private Double totalMrr;
    private List<PlanDistributionItem> items;

    @Data
    public static class PlanDistributionItem {
        private String planId;
        private String planCode;
        private String planName;
        private Integer companyCount;
        private Integer subscriptionCount;
        private Integer employeeCount;
        private Double mrr;
        private Double percentage;
    }
}
