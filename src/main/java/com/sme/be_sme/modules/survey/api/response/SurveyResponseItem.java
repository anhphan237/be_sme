package com.sme.be_sme.modules.survey.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class SurveyResponseItem {
    private String surveyInstanceId;
    private String surveyTemplateId;
    private String onboardingId;
    private String status;
    private Date sentAt;
    private Date closedAt;
}
