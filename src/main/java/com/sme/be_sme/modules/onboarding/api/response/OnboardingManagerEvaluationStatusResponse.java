package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Data;

@Data
public class OnboardingManagerEvaluationStatusResponse {

    private String instanceId;

    private String onboardingStatus;

    /**
     * PENDING | SENT | SUBMITTED | SKIPPED
     */
    private String managerEvaluationStatus;

    private String managerEvaluationSurveyInstanceId;

    private String managerUserId;

    private String managerName;

    private String targetEmployeeUserId;

    private String targetEmployeeName;

    private String targetEmployeeEmail;

    private String message;
}