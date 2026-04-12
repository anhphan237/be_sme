package com.sme.be_sme.modules.onboarding.service;

import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingTaskWorkflowNotificationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String TEMPLATE_TASK_PENDING_APPROVAL = "TASK_PENDING_APPROVAL";
    private static final String TEMPLATE_TASK_APPROVED = "TASK_APPROVED";
    private static final String TEMPLATE_TASK_REJECTED = "TASK_REJECTED";
    private static final String TEMPLATE_TASK_OVERDUE = "TASK_OVERDUE";

    private final NotificationService notificationService;
    private final OnboardingTaskApprovalAuthority approvalAuthority;

    public void notifyPendingApproval(TaskInstanceEntity task, String companyId) {
        String recipientUserId = resolveApprover(companyId, task);
        if (!StringUtils.hasText(recipientUserId)) {
            return;
        }
        String taskTitle = safeTaskTitle(task);
        String dueStr = dueAsString(task);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("taskTitle", taskTitle);
        placeholders.put("dueDate", dueStr);
        NotificationCreateParams params = NotificationCreateParams.builder()
                .companyId(companyId)
                .userId(recipientUserId)
                .type("TASK_PENDING_APPROVAL")
                .title("Task pending approval: " + taskTitle)
                .content("A task is waiting for your approval. Due: " + dueStr)
                .refType("TASK")
                .refId(task.getTaskId())
                .sendEmail(true)
                .emailTemplate(TEMPLATE_TASK_PENDING_APPROVAL)
                .emailPlaceholders(placeholders)
                .build();
        safeNotify(params);
    }

    public void notifyApproved(TaskInstanceEntity task, String companyId) {
        if (!StringUtils.hasText(task.getAssignedUserId())) {
            return;
        }
        String taskTitle = safeTaskTitle(task);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("taskTitle", taskTitle);
        NotificationCreateParams params = NotificationCreateParams.builder()
                .companyId(companyId)
                .userId(task.getAssignedUserId().trim())
                .type("TASK_APPROVED")
                .title("Task approved: " + taskTitle)
                .content("Your task has been approved.")
                .refType("TASK")
                .refId(task.getTaskId())
                .sendEmail(true)
                .emailTemplate(TEMPLATE_TASK_APPROVED)
                .emailPlaceholders(placeholders)
                .build();
        safeNotify(params);
    }

    public void notifyRejected(TaskInstanceEntity task, String companyId, String reason) {
        if (!StringUtils.hasText(task.getAssignedUserId())) {
            return;
        }
        String taskTitle = safeTaskTitle(task);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("taskTitle", taskTitle);
        placeholders.put("reason", StringUtils.hasText(reason) ? reason.trim() : "");
        NotificationCreateParams params = NotificationCreateParams.builder()
                .companyId(companyId)
                .userId(task.getAssignedUserId().trim())
                .type("TASK_REJECTED")
                .title("Task rejected: " + taskTitle)
                .content("Your task was rejected. " + (StringUtils.hasText(reason) ? ("Reason: " + reason.trim()) : ""))
                .refType("TASK")
                .refId(task.getTaskId())
                .sendEmail(true)
                .emailTemplate(TEMPLATE_TASK_REJECTED)
                .emailPlaceholders(placeholders)
                .build();
        safeNotify(params);
    }

    public void notifyOverdue(TaskInstanceEntity task, String companyId) {
        if (task == null || !StringUtils.hasText(task.getTaskId())) {
            return;
        }
        String taskTitle = safeTaskTitle(task);
        String dueStr = dueAsString(task);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("taskTitle", taskTitle);
        placeholders.put("dueDate", dueStr);

        if (StringUtils.hasText(task.getAssignedUserId())) {
            NotificationCreateParams assigneeParams = NotificationCreateParams.builder()
                    .companyId(companyId)
                    .userId(task.getAssignedUserId().trim())
                    .type("TASK_OVERDUE")
                    .title("Task overdue: " + taskTitle)
                    .content("Task is overdue since " + dueStr + ".")
                    .refType("TASK")
                    .refId(task.getTaskId())
                    .sendEmail(true)
                    .emailTemplate(TEMPLATE_TASK_OVERDUE)
                    .emailPlaceholders(placeholders)
                    .build();
            safeNotify(assigneeParams);
        }

        String managerUserId = resolveApprover(companyId, task);
        if (StringUtils.hasText(managerUserId)
                && (!StringUtils.hasText(task.getAssignedUserId())
                || !managerUserId.equals(task.getAssignedUserId().trim()))) {
            NotificationCreateParams managerParams = NotificationCreateParams.builder()
                    .companyId(companyId)
                    .userId(managerUserId)
                    .type("TASK_OVERDUE_ESCALATION")
                    .title("Escalation: task overdue")
                    .content("A task under your scope is overdue: " + taskTitle)
                    .refType("TASK")
                    .refId(task.getTaskId())
                    .sendEmail(true)
                    .emailTemplate(TEMPLATE_TASK_OVERDUE)
                    .emailPlaceholders(placeholders)
                    .build();
            safeNotify(managerParams);
        }
    }

    public void notifyScheduleProposed(TaskInstanceEntity task, String companyId, String proposerUserId) {
        String recipient = resolveApprover(companyId, task);
        if (!StringUtils.hasText(recipient)) {
            return;
        }
        if (StringUtils.hasText(proposerUserId) && recipient.equals(proposerUserId.trim())) {
            return;
        }
        NotificationCreateParams params = NotificationCreateParams.builder()
                .companyId(companyId)
                .userId(recipient)
                .type("TASK_SCHEDULE_PROPOSED")
                .title("Task schedule proposed")
                .content("A proposed schedule is waiting for your confirmation.")
                .refType("TASK")
                .refId(task.getTaskId())
                .sendEmail(false)
                .build();
        safeNotify(params);
    }

    public void notifyScheduleConfirmed(TaskInstanceEntity task, String companyId, String confirmerUserId) {
        if (!StringUtils.hasText(task.getScheduleProposedBy())) {
            return;
        }
        String recipient = task.getScheduleProposedBy().trim();
        if (StringUtils.hasText(confirmerUserId) && recipient.equals(confirmerUserId.trim())) {
            return;
        }
        NotificationCreateParams params = NotificationCreateParams.builder()
                .companyId(companyId)
                .userId(recipient)
                .type("TASK_SCHEDULE_CONFIRMED")
                .title("Task schedule confirmed")
                .content("Your proposed schedule has been confirmed.")
                .refType("TASK")
                .refId(task.getTaskId())
                .sendEmail(false)
                .build();
        safeNotify(params);
    }

    public void notifyScheduleRescheduled(TaskInstanceEntity task, String companyId, String actorUserId) {
        String recipient = resolveApprover(companyId, task);
        if (!StringUtils.hasText(recipient)) {
            return;
        }
        if (StringUtils.hasText(actorUserId) && recipient.equals(actorUserId.trim())) {
            return;
        }
        NotificationCreateParams params = NotificationCreateParams.builder()
                .companyId(companyId)
                .userId(recipient)
                .type("TASK_SCHEDULE_RESCHEDULED")
                .title("Task schedule updated")
                .content("A schedule was rescheduled and needs your confirmation.")
                .refType("TASK")
                .refId(task.getTaskId())
                .sendEmail(false)
                .build();
        safeNotify(params);
    }

    public void notifyScheduleCancelled(TaskInstanceEntity task, String companyId, String actorUserId) {
        if (!StringUtils.hasText(task.getScheduleProposedBy())) {
            return;
        }
        String recipient = task.getScheduleProposedBy().trim();
        if (StringUtils.hasText(actorUserId) && recipient.equals(actorUserId.trim())) {
            return;
        }
        NotificationCreateParams params = NotificationCreateParams.builder()
                .companyId(companyId)
                .userId(recipient)
                .type("TASK_SCHEDULE_CANCELLED")
                .title("Task schedule cancelled")
                .content("A scheduled task was cancelled.")
                .refType("TASK")
                .refId(task.getTaskId())
                .sendEmail(false)
                .build();
        safeNotify(params);
    }

    public void notifyNoShowCandidate(TaskInstanceEntity task, String companyId) {
        if (task == null || !StringUtils.hasText(task.getTaskId())) {
            return;
        }
        if (StringUtils.hasText(task.getAssignedUserId())) {
            NotificationCreateParams assignee = NotificationCreateParams.builder()
                    .companyId(companyId)
                    .userId(task.getAssignedUserId().trim())
                    .type("TASK_SCHEDULE_NO_SHOW_CANDIDATE")
                    .title("Scheduled task may be missed")
                    .content("Scheduled start time has passed. Please update task status.")
                    .refType("TASK")
                    .refId(task.getTaskId())
                    .sendEmail(false)
                    .build();
            safeNotify(assignee);
        }
        String confirmer = resolveApprover(companyId, task);
        if (StringUtils.hasText(confirmer)
                && (!StringUtils.hasText(task.getAssignedUserId())
                || !confirmer.equals(task.getAssignedUserId().trim()))) {
            NotificationCreateParams approver = NotificationCreateParams.builder()
                    .companyId(companyId)
                    .userId(confirmer)
                    .type("TASK_SCHEDULE_NO_SHOW_CANDIDATE")
                    .title("Possible no-show on scheduled task")
                    .content("A scheduled task has passed start time and is not completed.")
                    .refType("TASK")
                    .refId(task.getTaskId())
                    .sendEmail(false)
                    .build();
            safeNotify(approver);
        }
    }

    private String resolveApprover(String companyId, TaskInstanceEntity task) {
        if (task == null) {
            return null;
        }
        if (StringUtils.hasText(task.getApproverUserId())) {
            return task.getApproverUserId().trim();
        }
        OnboardingInstanceEntity instance = approvalAuthority.loadOnboardingInstance(companyId, task);
        return approvalAuthority.resolveLineManagerUserId(companyId, instance);
    }

    private static String safeTaskTitle(TaskInstanceEntity task) {
        return StringUtils.hasText(task != null ? task.getTitle() : null) ? task.getTitle().trim() : "Task";
    }

    private static String dueAsString(TaskInstanceEntity task) {
        if (task == null || task.getDueDate() == null) {
            return "";
        }
        return Instant.ofEpochMilli(task.getDueDate().getTime()).atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FMT);
    }

    private void safeNotify(NotificationCreateParams params) {
        try {
            notificationService.create(params);
        } catch (Exception e) {
            log.warn("Workflow notification failed for type {} ref {}: {}",
                    params.getType(), params.getRefId(), e.getMessage());
        }
    }
}

