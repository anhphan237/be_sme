package com.sme.be_sme.modules.onboarding.api.response;

import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskTimelineByOnboardingResponse {
    private String onboardingId;
    private Integer totalTasks;
    private List<AssigneeTimeline> assignees;

    @Getter
    @Setter
    public static class AssigneeTimeline {
        private String assigneeUserId;
        private String assigneeUserName;
        private Integer taskCount;
        private List<TaskItem> tasks;
    }

    @Getter
    @Setter
    public static class TaskItem {
        private String taskId;
        private String checklistId;
        private String checklistName;
        private String title;
        private String status;
        private Date dueDate;
        private Date scheduledStartAt;
        private Date scheduledEndAt;
        private String scheduleStatus;
        private String scheduleRescheduleReason;
        private String scheduleCancelReason;
        private String scheduleNoShowReason;
        private Date createdAt;
        private Long dueInHours;
        private Boolean overdue;
        private String dueCategory;
        private Boolean requireAck;
        private Boolean requiresManagerApproval;
        private String approvalStatus;
    }
}

