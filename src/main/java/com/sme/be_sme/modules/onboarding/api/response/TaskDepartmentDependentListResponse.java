package com.sme.be_sme.modules.onboarding.api.response;

import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskDepartmentDependentListResponse {
    private String departmentId;
    private Integer totalCount;
    private Integer page;
    private Integer size;
    private List<TaskItem> tasks;

    @Getter
    @Setter
    public static class TaskItem {
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
}
