package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

import java.util.List;

@Data
public class PlatformOnboardingAnalyticsResponse {
    private int totalOnboardings;
    private int completedOnboardings;
    private Double completionRate;
    private Double averageCompletionDays;
    private List<CompanyItem> companyItems;
    @Data
    public static class CompanyItem {
        private String companyId;
        private String companyName;
        private Integer totalOnboardings;
        private Integer completedOnboardings;
        private Integer cancelledOnboardings;
        private Integer riskOnboardings;
        private Double completionRate;
        private Double averageCompletionDays;
    }
}
