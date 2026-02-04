package com.sme.be_sme.modules.survey.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveySubmitResponse {
    private String surveyInstanceId;
    private String status;
}
