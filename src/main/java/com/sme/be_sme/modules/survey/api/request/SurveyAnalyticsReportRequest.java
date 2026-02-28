package com.sme.be_sme.modules.survey.api.request;

import lombok.Data;

import java.util.Date;

@Data
public class SurveyAnalyticsReportRequest {
    private Date startDate;
    private Date endDate;

    // optional filters
    private String templateId;   // filter 1 template
    private String stage;        // "D7" / "D30" / ...
}
