package com.sme.be_sme.modules.onboarding.api.response;

import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingInstanceDetailResponse {
    private String instanceId;
    private String employeeId;
    private String employeeUserId;
    private String managerUserId;
    private String managerName;
    private String templateId;
    private String status;
    private Date startDate;
    private Date completedAt;
    private Integer progressPercent;
    private List<ChecklistDetailItem> checklists;

    @Getter
    @Setter
    public static class ChecklistDetailItem {
        private String checklistId;
        private String name;
        private String stage;
        private String status;
        private Integer progressPercent;
        private List<TaskDetailItem> tasks;
    }

    @Getter
    @Setter
    public static class TaskDetailItem {
        private String taskId;
        private String title;
        private String status;
        private String assignedUserId;
        private Date dueDate;
        private Date completedAt;
    }
}
