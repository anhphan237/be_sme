package com.sme.be_sme.modules.survey.api.request;

import lombok.Data;

@Data
public class SurveyManagerEvaluationReportRequest {
    private String templateId;
    private String startDate;
    private String endDate;
    private String managerUserId;
    private String keyword;
}
