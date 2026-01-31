package com.sme.be_sme.modules.survey.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveyTemplateResponse {
    private String templateId;
    private String name;
    private String status;
}
