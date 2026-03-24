package com.sme.be_sme.modules.survey.infrastructure.persistence.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
public class SurveyResponseRow {
    private String surveyResponseId;
    private String surveyInstanceId;
    private String responderUserId;
    private OffsetDateTime submittedAt;
}
