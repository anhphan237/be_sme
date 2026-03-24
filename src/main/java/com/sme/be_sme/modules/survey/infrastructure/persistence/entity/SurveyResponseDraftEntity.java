package com.sme.be_sme.modules.survey.infrastructure.persistence.entity;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class SurveyResponseDraftEntity {
    private String surveyResponseDraftId;
    private String companyId;
    private String surveyInstanceId;
    private String questionId;
    private String responderUserId;
    private String answerValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
