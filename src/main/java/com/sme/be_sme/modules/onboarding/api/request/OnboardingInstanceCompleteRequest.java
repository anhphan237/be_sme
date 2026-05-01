package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingInstanceCompleteRequest {


    private String instanceId;

    /**
     * SEND_NOW | SEND_LATER
     * Default = SEND_NOW
     */
    private String managerEvaluationMode;

    private String managerEvaluationTemplateId;

    private Integer managerEvaluationDueDays;
}
