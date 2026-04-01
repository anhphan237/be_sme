package com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * Task row joined with checklist for assignee-scoped listing.
 */
@Getter
@Setter
public class TaskAssigneeListRow {

    private String taskId;
    private String companyId;
    private String checklistId;
    private String onboardingId;
    private String checklistName;

    private String title;
    private String description;
    private String status;
    private Date dueDate;
    private String assignedUserId;
    private String assignedDepartmentId;
    private Date completedAt;
    private Date createdAt;
    private Boolean requireAck;
    private Boolean requiresManagerApproval;
    private String approvalStatus;
    private String approverUserId;
    private Date scheduledStartAt;
    private Date scheduledEndAt;
    private String scheduleStatus;
    private String scheduleRescheduleReason;
    private String scheduleCancelReason;
    private String scheduleNoShowReason;
}
