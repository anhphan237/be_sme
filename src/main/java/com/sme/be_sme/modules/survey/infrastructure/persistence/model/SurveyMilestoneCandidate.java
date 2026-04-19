package com.sme.be_sme.modules.survey.infrastructure.persistence.model;

import lombok.Data;

import java.util.Date;

@Data
public class SurveyMilestoneCandidate {
    private String surveyInstanceId;

    private String companyId;
    private String onboardingId;

    private String employeeId;
    private String employeeUserId;
    private String managerUserId;

    private String templateId;
    private String templateName;
    private String stage;
    private String targetRole;

    private String responderUserId;

    private Date scheduledAt;
    private Date closedAt;
}