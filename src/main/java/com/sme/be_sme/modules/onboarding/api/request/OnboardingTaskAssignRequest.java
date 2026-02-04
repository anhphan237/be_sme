package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingTaskAssignRequest {
    private String taskId;
    private String assigneeUserId;
}
