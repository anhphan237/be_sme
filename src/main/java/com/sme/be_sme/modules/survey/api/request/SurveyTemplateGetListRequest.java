package com.sme.be_sme.modules.survey.api.request;

import lombok.Data;

@Data
public class SurveyTemplateGetListRequest {
    private String status;
    private String stage;      // D7 | D30 | D60
    private Boolean managerOnly;
}

