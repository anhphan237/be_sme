package com.sme.be_sme.modules.survey.api.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class SurveyScheduleRequest {
    private String templateId;
    private String onboardingId;
    private Integer milestoneDays;
    private Integer dueDays;
    private Date joinDate;

}
