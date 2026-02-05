package com.sme.be_sme.modules.survey.api.request;

import lombok.Data;

@Data
public class SurveyTemplateUpdateRequest {
    private String templateId;

    private String name;
    private String description;
    private String stage;        // D7/D30/D60...
    private Boolean managerOnly;
    private String status;       // DRAFT/ACTIVE/...
    private Boolean isDefault;
    private Integer version;
}
