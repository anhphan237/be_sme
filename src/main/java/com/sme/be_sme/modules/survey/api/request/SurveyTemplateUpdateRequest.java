package com.sme.be_sme.modules.survey.api.request;

import lombok.Data;

@Data
public class SurveyTemplateUpdateRequest {
    private String templateId;

    private String name;
    private String description;
    private String stage;
    private Boolean managerOnly;
    private String status;
    private Boolean isDefault;
    private Integer version;
    private String targetRole;
    private Boolean forceReplaceDefault;
}
