package com.sme.be_sme.modules.survey.infrastructure.persistence.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
public class SurveyInstanceDetailRow {
    private String surveyInstanceId;
    private String surveyTemplateId;
    private String templateName;
    private String responderUserId;
    private String status;
    private OffsetDateTime scheduledAt;
}
