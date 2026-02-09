package com.sme.be_sme.modules.survey.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class SurveyScheduleResponse {
    private String scheduleId;
    private String status;
    private Date openAt;
    private Date dueAt;
    private String templateId;
    private String responderUserId;
    private String instanceId;

}
