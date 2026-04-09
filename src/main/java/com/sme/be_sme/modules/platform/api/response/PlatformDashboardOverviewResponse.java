package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

@Data
public class PlatformDashboardOverviewResponse {
    private String startDate;
    private String endDate;
    private String groupBy;
    private Integer totalCompanies;
    private Double companyGrowthRate;
    private Double mrr;
    private Double mrrGrowthRate;
    private Integer activeOnboardings;
    private Double activeOnboardingsGrowthRate;
    private Integer riskOnboardings;
    private Double riskOnboardingsGrowthRate;
    private Integer totalEmployees;
    private Double employeeGrowthRate;
}
