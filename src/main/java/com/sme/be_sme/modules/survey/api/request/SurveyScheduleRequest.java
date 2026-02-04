package com.sme.be_sme.modules.survey.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveyScheduleRequest {
    private String templateId;
    private Integer milestoneDays;
}
