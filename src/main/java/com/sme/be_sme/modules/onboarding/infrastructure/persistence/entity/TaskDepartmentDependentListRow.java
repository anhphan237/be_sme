package com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskDepartmentDependentListRow {
    private String taskId;
    private String onboardingId;
    private String checklistId;
    private String checklistName;
    private String title;
    private String taskStatus;
    private Date dueDate;
    private String assignedUserId;
    private String assignedDepartmentId;
    private String checkpointStatus;
    private Boolean requireEvidence;
    private Date confirmedAt;
}
