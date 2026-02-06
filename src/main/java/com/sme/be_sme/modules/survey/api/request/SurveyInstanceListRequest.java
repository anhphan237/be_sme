package com.sme.be_sme.modules.survey.api.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class SurveyInstanceListRequest {
    /** Filter by survey template id */
    private String templateId;
    /** Filter by instance status (e.g. SCHEDULED, SENT, CLOSED) */
    private String status;
    /** Filter: scheduled_at >= startDate */
    private Date startDate;
    /** Filter: scheduled_at <= endDate */
    private Date endDate;
    /** Page size, default 20 */
    private Integer limit = 20;
    /** Offset for pagination, default 0 */
    private Integer offset = 0;
}
