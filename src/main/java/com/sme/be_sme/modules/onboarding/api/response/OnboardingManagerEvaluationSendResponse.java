package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Data;

@Data
public class OnboardingManagerEvaluationSendResponse {

    private String instanceId;

    /**
     * SENT | SKIPPED
     */
    private String managerEvaluationStatus;

    private String managerEvaluationSurveyInstanceId;

    private String managerEvaluationMessage;
}