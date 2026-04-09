package com.sme.be_sme.modules.platform.api.response;

import java.util.List;
import lombok.Data;

@Data
public class PlatformRiskDashboardResponse {
    private Integer riskOnboardings;
    private Integer failedPayments;
    private Integer suspendedCompanies;
    private Integer companiesNearPlanLimit;
    private Integer expiringSubscriptions;
    private Integer lowCompletionCompanies;
    private List<RiskCompanyItem> lowCompletionCompanyItems;

    @Data
    public static class RiskCompanyItem {
        private String companyId;
        private String companyName;
        private Double completionRate;
    }
}
