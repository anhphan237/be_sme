package com.sme.be_sme.modules.survey.infrastructure.persistence.model;

import lombok.Data;

@Data
public class SurveyResponseDraftRow {
    private String surveyResponseDraftId;
    private String surveyInstanceId;
    private String questionId;
    private String responderUserId;
    private String answerValue;
}

