package com.sme.be_sme.modules.onboarding.infrastructure.persistence.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskTemplateRow {
    private String taskTemplateId;
    private String checklistTemplateId;
    private String name;
    private String description;
    private String ownerType;
    private String ownerRefId;
    private Integer dueDaysOffset;
    private Boolean requireAck;
    private Integer orderNo;
    private String status;
}
