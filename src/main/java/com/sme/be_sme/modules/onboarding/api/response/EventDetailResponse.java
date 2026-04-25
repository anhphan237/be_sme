package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class EventDetailResponse {
    private String eventInstanceId;
    private String eventTemplateId;
    private Date eventAt;
    private String sourceType;
    private List<String> sourceDepartmentIds;
    private List<String> sourceUserIds;
    private List<String> participantUserIds;
    private String status;
    private Date notifiedAt;
    private String createdBy;
    private Date createdAt;
    private Date updatedAt;
    private EventTemplateInfo eventTemplate;
    private ChecklistInfo checklist;
    private List<TaskItem> tasks;

    @Getter
    @Setter
    public static class EventTemplateInfo {
        private String eventTemplateId;
        private String name;
        private String description;
        private String content;
        private String status;
    }

    @Getter
    @Setter
    public static class ChecklistInfo {
        private String checklistId;
        private String name;
        private String stage;
        private String status;
        private Integer progressPercent;
        private Date openAt;
        private Date deadlineAt;
    }

    @Getter
    @Setter
    public static class TaskItem {
        private String taskId;
        private String checklistId;
        private String title;
        private String description;
        private String status;
        private Date dueDate;
        private String assignedUserId;
        private String assignedDepartmentId;
        private Date completedAt;
        private Date createdAt;
        private Date updatedAt;
        private Date scheduledStartAt;
        private Date scheduledEndAt;
        private String scheduleStatus;
    }
}
