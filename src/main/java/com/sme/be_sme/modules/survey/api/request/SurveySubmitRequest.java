package com.sme.be_sme.modules.survey.api.request;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveySubmitRequest {
    private String surveyInstanceId;
    private Map<String, String> answers;
}
