package com.sme.be_sme.modules.survey.api.response;

import lombok.Data;

@Data
public class SurveyTemplateDeleteResponse {
    private String templateId;
    private Boolean deleted;
}
