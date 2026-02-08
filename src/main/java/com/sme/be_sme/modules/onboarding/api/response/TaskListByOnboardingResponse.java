package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Date;

@Getter
@Setter
public class TaskListByOnboardingResponse {
    private String onboardingId;
    private List<TaskItem> tasks;
    private Integer totalCount;
    private Integer page;
    private Integer size;

    @Getter
    @Setter
    public static class TaskItem {
        private String taskId;
        private String checklistId;
        private String checklistName;      // Tên checklist chứa task
        private String title;
        private String description;
        private String status;
        private Date dueDate;
        private String assignedUserId;
        private String assignedUserName;    // Enriched - Tên user được gán
        private String assignedDepartmentId;
        private Date completedAt;
        private Date createdAt;
    }
}
