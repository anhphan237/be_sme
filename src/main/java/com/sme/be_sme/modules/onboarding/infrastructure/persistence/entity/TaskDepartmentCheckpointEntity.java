package com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class TaskDepartmentCheckpointEntity {
    private String taskDepartmentCheckpointId;
    private String companyId;
    private String taskId;
    private String departmentId;
    private String status;
    private Boolean requireEvidence;
    private String evidenceNote;
    private String evidenceRef;
    private String confirmedBy;
    private Date confirmedAt;
    private Date createdAt;
    private Date updatedAt;
}
