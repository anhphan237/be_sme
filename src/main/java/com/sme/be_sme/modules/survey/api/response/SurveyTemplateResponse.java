package com.sme.be_sme.modules.survey.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class SurveyTemplateResponse {
    private String templateId;
    private String companyId;
    private String name;
    private String status;
    private String description;
    private String stage;
    private Boolean managerOnly;
    private Integer version;
    private String createdBy;
    private Date createdAt;
    private Date updatedAt;
    private Boolean isDefault;
}
