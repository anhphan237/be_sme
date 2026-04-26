package com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class TaskTemplateDepartmentCheckpointEntity {
    private String taskTemplateDepartmentCheckpointId;
    private String companyId;
    private String taskTemplateId;
    private String departmentId;
    private Boolean requireEvidence;
    private Integer sortOrder;
    private String status;
    private Date createdAt;
    private Date updatedAt;
}
