package com.sme.be_sme.modules.onboarding.api.response;

import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskListByAssigneeResponse {

    private String assigneeUserId;
    private List<TaskItem> tasks;
    private Integer totalCount;
    private Integer page;
    private Integer size;

    @Getter
    @Setter
    public static class TaskItem {
        private String taskId;
        private String onboardingId;
        private String checklistId;
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
    }
}
