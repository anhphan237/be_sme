package com.sme.be_sme.modules.survey.api.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class SurveySatisfactionReportRequest {
    /** Filter by survey template id (optional) */
    private String templateId;
    /** Filter by stage: 7, 30, or 60 (optional) */
    private Integer stage;
    /** Filter: submitted_at >= startDate (optional) */
    private Date startDate;
    /** Filter: submitted_at <= endDate (optional) */
    private Date endDate;
}
