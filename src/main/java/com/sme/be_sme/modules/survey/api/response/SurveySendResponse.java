package com.sme.be_sme.modules.survey.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class SurveySendResponse {
    private String surveyInstanceId;
    private String status;   // SENT
    private Date sentAt;
}
