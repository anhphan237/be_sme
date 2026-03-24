package com.sme.be_sme.modules.survey.infrastructure.persistence.model;

import lombok.Data;

@Data
public class SurveyAnswerRow {
    private String surveyAnswerId;
    private String surveyResponseId;
    private String surveyQuestionId;

    private String valueText;
    private Integer valueRating;
    private String valueChoice;
}
