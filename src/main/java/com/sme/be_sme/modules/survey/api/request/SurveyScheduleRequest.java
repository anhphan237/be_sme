package com.sme.be_sme.modules.survey.api.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class SurveyScheduleRequest {
    private String onboardingId;
    private String templateId;
    private Date scheduledAt;
    private Integer dueDays;

}
