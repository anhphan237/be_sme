package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class TaskDetailResponse {
    // Basic task info
    private String taskId;
    private String title;
    private String description;
    private String status;
    private Date dueDate;
    private Long dueInHours;
    private Boolean overdue;
    private String dueCategory;
    private Date completedAt;
    private Date createdAt;
    private Date updatedAt;
    private String createdBy;
    private String reporterUserId;

    private Boolean requireAck;
    private Boolean requireDoc;
    private Date acknowledgedAt;
    private String acknowledgedBy;
    private Boolean requiresManagerApproval;
    private String approvalStatus;
    private String approvedBy;
    private Date approvedAt;
    private String rejectionReason;
    private String approverUserId;
    private Date scheduledStartAt;
    private Date scheduledEndAt;
    private String scheduleStatus;
    private String scheduleProposedBy;
    private Date scheduleProposedAt;
    private String scheduleConfirmedBy;
    private Date scheduleConfirmedAt;
    private String scheduleRescheduleReason;
    private String scheduleCancelReason;
    private String scheduleNoShowReason;

    // Related entities (enriched)
    private ChecklistInfo checklist;
    private UserInfo assignedUser;
    private UserInfo createdByUser;
    private UserInfo reporterUser;
    private DepartmentInfo assignedDepartment;

    // Related collections
    private List<RequiredDocumentItem> requiredDocuments;
    private List<CommentItem> comments;
    private List<AttachmentItem> attachments;
    private List<ActivityLogItem> activityLogs;
    private List<AllLogItem> allLogs;

    // Nested classes
    @Getter
    @Setter
    public static class ChecklistInfo {
        private String checklistId;
        private String name;
        private String stage;
        private String onboardingId;
    }

    @Getter
    @Setter
    public static class UserInfo {
        private String userId;
        private String fullName;
        private String email;
    }

    @Getter
    @Setter
    public static class DepartmentInfo {
        private String departmentId;
        private String name;
    }

    @Getter
    @Setter
    public static class CommentItem {
        private String commentId;
        private String parentCommentId;
        private String content;
        private String createdBy;
        private String createdByName;
        private Date createdAt;
    }

    @Getter
    @Setter
    public static class AttachmentItem {
        private String attachmentId;
        private String fileName;
        private String fileUrl;
        private String fileType;
        private Long fileSizeBytes;
        private String uploadedBy;
        private String uploadedByName;
        private Date uploadedAt;
    }

    @Getter
    @Setter
    public static class RequiredDocumentItem {
        private String documentId;
        private String title;
    }

    @Getter
    @Setter
    public static class ActivityLogItem {
        private String logId;
        private String action;
        private String oldValue;
        private String newValue;
        private String actorUserId;
        private String actorName;
        private Date createdAt;
    }

    @Getter
    @Setter
    public static class AllLogItem {
        private String type; // COMMENT | HISTORY
        private Date createdAt;
        private String actorUserId;
        private String actorName;

        // COMMENT payload
        private String commentId;
        private String parentCommentId;
        private String content;

        // HISTORY payload
        private String logId;
        private String action;
        private String oldValue;
        private String newValue;
    }
}
