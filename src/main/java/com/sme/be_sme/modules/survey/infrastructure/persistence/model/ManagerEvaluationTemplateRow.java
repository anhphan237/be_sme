package com.sme.be_sme.modules.survey.infrastructure.persistence.model;
import lombok.Data;

@Data
public class ManagerEvaluationTemplateRow {

    private String surveyTemplateId;
    private String companyId;
    private String name;
    private String purpose;
    private String stage;
    private String targetRole;
    private String status;
    private Boolean isDefault;
    private Integer questionCount;
}