package com.sme.be_sme.modules.onboarding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskActivityLogMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskActivityLogEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.shared.util.UuidGenerator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OnboardingTaskActivityLogService {

    public static final String ACTION_STATUS_CHANGED = "STATUS_CHANGED";
    public static final String ACTION_ACKNOWLEDGED = "ACKNOWLEDGED";
    public static final String ACTION_APPROVED = "APPROVED";
    public static final String ACTION_REJECTED = "REJECTED";
    public static final String ACTION_ASSIGNED = "ASSIGNED";
    public static final String ACTION_SCHEDULE_PROPOSED = "SCHEDULE_PROPOSED";
    public static final String ACTION_SCHEDULE_CONFIRMED = "SCHEDULE_CONFIRMED";
    public static final String ACTION_SCHEDULE_RESCHEDULED = "SCHEDULE_RESCHEDULED";
    public static final String ACTION_SCHEDULE_CANCELLED = "SCHEDULE_CANCELLED";
    public static final String ACTION_SCHEDULE_NO_SHOW = "SCHEDULE_NO_SHOW";
    public static final String ACTION_DEPARTMENT_CONFIRMED = "DEPARTMENT_CONFIRMED";

    private final TaskActivityLogMapper taskActivityLogMapper;
    private final ObjectMapper objectMapper;

    public void logStatusChanged(TaskInstanceEntity before, TaskInstanceEntity after, String actorUserId) {
        Map<String, Object> oldValue = new LinkedHashMap<>();
        oldValue.put("status", safe(before != null ? before.getStatus() : null));
        oldValue.put("approvalStatus", safe(before != null ? before.getApprovalStatus() : null));
        oldValue.put("completedAt", before != null ? before.getCompletedAt() : null);

        Map<String, Object> newValue = new LinkedHashMap<>();
        newValue.put("status", safe(after != null ? after.getStatus() : null));
        newValue.put("approvalStatus", safe(after != null ? after.getApprovalStatus() : null));
        newValue.put("completedAt", after != null ? after.getCompletedAt() : null);

        insert(after, actorUserId, ACTION_STATUS_CHANGED, toJson(oldValue), toJson(newValue));
    }

    public void logAcknowledged(TaskInstanceEntity task, String actorUserId, Date acknowledgedAt) {
        Map<String, Object> oldValue = new LinkedHashMap<>();
        oldValue.put("status", null);
        oldValue.put("acknowledgedAt", null);
        oldValue.put("acknowledgedBy", null);

        Map<String, Object> newValue = new LinkedHashMap<>();
        newValue.put("status", safe(task.getStatus()));
        newValue.put("acknowledgedAt", acknowledgedAt);
        newValue.put("acknowledgedBy", actorUserId);

        insert(task, actorUserId, ACTION_ACKNOWLEDGED, toJson(oldValue), toJson(newValue));
    }

    public void logApproved(TaskInstanceEntity before, TaskInstanceEntity after, String actorUserId) {
        Map<String, Object> oldValue = new LinkedHashMap<>();
        oldValue.put("status", safe(before != null ? before.getStatus() : null));
        oldValue.put("approvalStatus", safe(before != null ? before.getApprovalStatus() : null));

        Map<String, Object> newValue = new LinkedHashMap<>();
        newValue.put("status", safe(after != null ? after.getStatus() : null));
        newValue.put("approvalStatus", safe(after != null ? after.getApprovalStatus() : null));
        newValue.put("approvedBy", actorUserId);
        newValue.put("approvedAt", after != null ? after.getApprovedAt() : null);

        insert(after, actorUserId, ACTION_APPROVED, toJson(oldValue), toJson(newValue));
    }

    public void logRejected(TaskInstanceEntity before, TaskInstanceEntity after, String actorUserId, String reason) {
        Map<String, Object> oldValue = new LinkedHashMap<>();
        oldValue.put("status", safe(before != null ? before.getStatus() : null));
        oldValue.put("approvalStatus", safe(before != null ? before.getApprovalStatus() : null));

        Map<String, Object> newValue = new LinkedHashMap<>();
        newValue.put("status", safe(after != null ? after.getStatus() : null));
        newValue.put("approvalStatus", safe(after != null ? after.getApprovalStatus() : null));
        newValue.put("reason", safe(reason));

        insert(after, actorUserId, ACTION_REJECTED, toJson(oldValue), toJson(newValue));
    }

    public void logAssigned(TaskInstanceEntity before, TaskInstanceEntity after, String actorUserId) {
        Map<String, Object> oldValue = new LinkedHashMap<>();
        oldValue.put("assignedUserId", safe(before != null ? before.getAssignedUserId() : null));
        oldValue.put("status", safe(before != null ? before.getStatus() : null));

        Map<String, Object> newValue = new LinkedHashMap<>();
        newValue.put("assignedUserId", safe(after != null ? after.getAssignedUserId() : null));
        newValue.put("status", safe(after != null ? after.getStatus() : null));

        insert(after, actorUserId, ACTION_ASSIGNED, toJson(oldValue), toJson(newValue));
    }

    public void logScheduleProposed(TaskInstanceEntity before, TaskInstanceEntity after, String actorUserId) {
        Map<String, Object> oldValue = new LinkedHashMap<>();
        oldValue.put("scheduleStatus", safe(before != null ? before.getScheduleStatus() : null));
        oldValue.put("scheduledStartAt", before != null ? before.getScheduledStartAt() : null);
        oldValue.put("scheduledEndAt", before != null ? before.getScheduledEndAt() : null);
        oldValue.put("scheduleProposedBy", safe(before != null ? before.getScheduleProposedBy() : null));

        Map<String, Object> newValue = new LinkedHashMap<>();
        newValue.put("scheduleStatus", safe(after != null ? after.getScheduleStatus() : null));
        newValue.put("scheduledStartAt", after != null ? after.getScheduledStartAt() : null);
        newValue.put("scheduledEndAt", after != null ? after.getScheduledEndAt() : null);
        newValue.put("scheduleProposedBy", safe(after != null ? after.getScheduleProposedBy() : null));

        insert(after, actorUserId, ACTION_SCHEDULE_PROPOSED, toJson(oldValue), toJson(newValue));
    }

    public void logScheduleConfirmed(TaskInstanceEntity before, TaskInstanceEntity after, String actorUserId) {
        Map<String, Object> oldValue = new LinkedHashMap<>();
        oldValue.put("scheduleStatus", safe(before != null ? before.getScheduleStatus() : null));
        oldValue.put("scheduleConfirmedBy", safe(before != null ? before.getScheduleConfirmedBy() : null));
        oldValue.put("scheduleConfirmedAt", before != null ? before.getScheduleConfirmedAt() : null);

        Map<String, Object> newValue = new LinkedHashMap<>();
        newValue.put("scheduleStatus", safe(after != null ? after.getScheduleStatus() : null));
        newValue.put("scheduleConfirmedBy", safe(after != null ? after.getScheduleConfirmedBy() : null));
        newValue.put("scheduleConfirmedAt", after != null ? after.getScheduleConfirmedAt() : null);

        insert(after, actorUserId, ACTION_SCHEDULE_CONFIRMED, toJson(oldValue), toJson(newValue));
    }

    public void logScheduleRescheduled(TaskInstanceEntity before, TaskInstanceEntity after, String actorUserId) {
        Map<String, Object> oldValue = new LinkedHashMap<>();
        oldValue.put("scheduleStatus", safe(before != null ? before.getScheduleStatus() : null));
        oldValue.put("scheduledStartAt", before != null ? before.getScheduledStartAt() : null);
        oldValue.put("scheduledEndAt", before != null ? before.getScheduledEndAt() : null);
        oldValue.put("reason", safe(before != null ? before.getScheduleRescheduleReason() : null));

        Map<String, Object> newValue = new LinkedHashMap<>();
        newValue.put("scheduleStatus", safe(after != null ? after.getScheduleStatus() : null));
        newValue.put("scheduledStartAt", after != null ? after.getScheduledStartAt() : null);
        newValue.put("scheduledEndAt", after != null ? after.getScheduledEndAt() : null);
        newValue.put("reason", safe(after != null ? after.getScheduleRescheduleReason() : null));

        insert(after, actorUserId, ACTION_SCHEDULE_RESCHEDULED, toJson(oldValue), toJson(newValue));
    }

    public void logScheduleCancelled(TaskInstanceEntity before, TaskInstanceEntity after, String actorUserId) {
        Map<String, Object> oldValue = new LinkedHashMap<>();
        oldValue.put("scheduleStatus", safe(before != null ? before.getScheduleStatus() : null));
        oldValue.put("reason", safe(before != null ? before.getScheduleCancelReason() : null));

        Map<String, Object> newValue = new LinkedHashMap<>();
        newValue.put("scheduleStatus", safe(after != null ? after.getScheduleStatus() : null));
        newValue.put("reason", safe(after != null ? after.getScheduleCancelReason() : null));

        insert(after, actorUserId, ACTION_SCHEDULE_CANCELLED, toJson(oldValue), toJson(newValue));
    }

    public void logScheduleNoShow(TaskInstanceEntity before, TaskInstanceEntity after, String actorUserId) {
        Map<String, Object> oldValue = new LinkedHashMap<>();
        oldValue.put("scheduleStatus", safe(before != null ? before.getScheduleStatus() : null));
        oldValue.put("reason", safe(before != null ? before.getScheduleNoShowReason() : null));

        Map<String, Object> newValue = new LinkedHashMap<>();
        newValue.put("scheduleStatus", safe(after != null ? after.getScheduleStatus() : null));
        newValue.put("reason", safe(after != null ? after.getScheduleNoShowReason() : null));

        insert(after, actorUserId, ACTION_SCHEDULE_NO_SHOW, toJson(oldValue), toJson(newValue));
    }

    public void logDepartmentConfirmed(
            TaskInstanceEntity task,
            String actorUserId,
            String departmentId,
            String oldStatus,
            String newStatus,
            String evidenceNote,
            String evidenceRef) {
        Map<String, Object> oldValue = new LinkedHashMap<>();
        oldValue.put("departmentId", safe(departmentId));
        oldValue.put("status", safe(oldStatus));

        Map<String, Object> newValue = new LinkedHashMap<>();
        newValue.put("departmentId", safe(departmentId));
        newValue.put("status", safe(newStatus));
        newValue.put("evidenceNote", safe(evidenceNote));
        newValue.put("evidenceRef", safe(evidenceRef));

        insert(task, actorUserId, ACTION_DEPARTMENT_CONFIRMED, toJson(oldValue), toJson(newValue));
    }

    private void insert(TaskInstanceEntity task, String actorUserId, String action, String oldValue, String newValue) {
        if (task == null || !StringUtils.hasText(task.getCompanyId()) || !StringUtils.hasText(task.getTaskId())) {
            return;
        }
        TaskActivityLogEntity entity = new TaskActivityLogEntity();
        entity.setTaskActivityLogId(UuidGenerator.generate());
        entity.setCompanyId(task.getCompanyId());
        entity.setTaskId(task.getTaskId());
        entity.setActorUserId(StringUtils.hasText(actorUserId) ? actorUserId.trim() : "system");
        entity.setAction(action);
        entity.setOldValue(oldValue);
        entity.setNewValue(newValue);
        entity.setCreatedAt(new Date());
        taskActivityLogMapper.insert(entity);
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private static String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

