package com.sme.be_sme.modules.survey.infrastructure.persistence.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SurveyResponseFilterRow {

    private String surveyResponseId;
    private String surveyInstanceId;
    private String surveyTemplateId;
    private String templateName;

    private String onboardingId;
    private String stage;

    private String employeeId;
    private String employeeUserId;
    private String employeeName;
    private String employeeEmail;
    private String jobTitle;
    private String departmentName;

    private String managerUserId;
    private String managerName;

    private BigDecimal overallScore;
    private Date submittedAt;
}