package com.sme.be_sme.modules.platform.api.response;

import lombok.Data;

@Data
public class PlatformOnboardingAnalyticsResponse {
    private int totalOnboardings;
    private int completedOnboardings;
    private Double completionRate;
    private Double averageCompletionDays;
}
