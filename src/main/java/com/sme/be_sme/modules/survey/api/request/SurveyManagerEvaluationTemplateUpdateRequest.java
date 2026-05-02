package com.sme.be_sme.modules.survey.api.request;

import lombok.Data;

@Data
public class SurveyManagerEvaluationTemplateUpdateRequest {

    private String templateId;

    private String name;

    private String description;

    private Boolean isDefault;

    private Boolean forceReplaceDefault;
}