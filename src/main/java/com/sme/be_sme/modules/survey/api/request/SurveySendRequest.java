package com.sme.be_sme.modules.survey.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveySendRequest {
    private String surveyInstanceId;
    private String templateId;
    private String onboardingId;
    private String targetRole;
    private String responderUserId;
}
