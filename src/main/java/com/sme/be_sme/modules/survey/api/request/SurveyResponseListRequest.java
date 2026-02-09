package com.sme.be_sme.modules.survey.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveyResponseListRequest {
    private String templateId;      // optional
    private String onboardingId;    // optional
    private String status;          // SENT / CLOSED
}
