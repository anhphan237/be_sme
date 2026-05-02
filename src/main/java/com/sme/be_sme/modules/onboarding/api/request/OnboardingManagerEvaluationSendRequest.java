package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Data;

@Data
public class OnboardingManagerEvaluationSendRequest {

    private String instanceId;

    private String managerEvaluationTemplateId;

    private Integer managerEvaluationDueDays;
}