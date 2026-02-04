package com.sme.be_sme.modules.survey.api.response;

import lombok.Data;

import java.util.List;
@Data
public class SurveyTemplateDetailResponse {
    private String templateId;
    private String name;
    private String description;
    private String status;
    private String stage;
    private Boolean managerOnly;
    private Integer version;

    private List<SurveyQuestionResponse> questions;
}
