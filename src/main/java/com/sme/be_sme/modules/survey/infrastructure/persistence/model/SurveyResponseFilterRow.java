package com.sme.be_sme.modules.survey.infrastructure.persistence.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class SurveyResponseFilterRow {
    private String surveyResponseId;
    private String surveyInstanceId;
    private String surveyTemplateId;
    private BigDecimal overallScore;
    private Date submittedAt;
}
