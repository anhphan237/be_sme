package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingTaskGenerationResponse {
    private String instanceId;
    private int totalTasks;
}
