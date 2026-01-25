package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingTaskResponse {
    private String taskId;
    private String assigneeUserId;
    private String status;
}
