package com.sme.be_sme.modules.survey.infrastructure.persistence.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class SurveyInstanceListRow {
    private String surveyInstanceId;
    private String surveyTemplateId;
    private String templateName;
    private Date scheduledAt;
    private String status;
    private Date createdAt;
}
