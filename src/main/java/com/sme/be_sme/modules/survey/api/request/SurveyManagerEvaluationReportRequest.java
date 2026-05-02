package com.sme.be_sme.modules.survey.api.request;

import lombok.Data;

@Data
public class SurveyManagerEvaluationReportRequest {

    private String templateId;

    private String startDate;

    private String endDate;

    private String managerUserId;

    private String keyword;

    /**
     * PENDING | SENT | SUBMITTED | EXPIRED
     */
    private String status;

    /**
     * FIT | FOLLOW_UP | NOT_FIT | NOT_EVALUATED
     */
    private String fitLevel;
}
