package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

@Data
public class PlatformUsageAnalyticsResponse {
    private Integer totalOnboardings;
    private Integer totalCompletedOnboardings;
    private Integer totalSurveyResponses;
    private Integer totalFeedbacks;
    private Double avgOnboardingsPerCompany;
}
