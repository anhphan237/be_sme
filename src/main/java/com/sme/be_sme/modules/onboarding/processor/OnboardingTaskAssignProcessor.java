package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskAssignRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskResponse;
import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskActivityLogService;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskWorkflow;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class OnboardingTaskAssignProcessor extends BaseBizProcessor<BizContext> {

    private static final String TEMPLATE_TASK_ASSIGNED = "TASK_ASSIGNED";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ObjectMapper objectMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final NotificationService notificationService;
    private final OnboardingTaskActivityLogService activityLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingTaskAssignRequest request = objectMapper.convertValue(payload, OnboardingTaskAssignRequest.class);
        validate(context, request);

        TaskInstanceEntity task = taskInstanceMapper.selectByPrimaryKey(request.getTaskId().trim());
        if (task == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "task not found");
        }
        if (!context.getTenantId().equals(task.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "task does not belong to tenant");
        }
        if (OnboardingTaskWorkflow.STATUS_DONE.equalsIgnoreCase(task.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "cannot reassign a completed task");
        }

        TaskInstanceEntity before = snapshot(task);
        task.setAssignedUserId(request.getAssigneeUserId().trim());
        task.setStatus(OnboardingTaskWorkflow.STATUS_ASSIGNED);
        task.setUpdatedAt(new Date());

        int updated = taskInstanceMapper.updateByPrimaryKey(task);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "assign task failed");
        }
        activityLogService.logAssigned(before, task, context.getOperatorId());
        activityLogService.logStatusChanged(before, task, context.getOperatorId());

        String taskTitle = StringUtils.hasText(task.getTitle()) ? task.getTitle() : "Task";
        String dueStr = task.getDueDate() != null
                ? Instant.ofEpochMilli(task.getDueDate().getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DATE_FMT)
                : "";
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("taskTitle", taskTitle);
        placeholders.put("dueDate", dueStr);
        NotificationCreateParams params = NotificationCreateParams.builder()
                .companyId(task.getCompanyId())
                .userId(task.getAssignedUserId())
                .type("TASK_ASSIGNED")
                .title("New task assigned: " + taskTitle)
                .content("You have been assigned: \"" + taskTitle + "\". Due: " + dueStr)
                .refType("TASK")
                .refId(task.getTaskId())
                .sendEmail(true)
                .emailTemplate(TEMPLATE_TASK_ASSIGNED)
                .emailPlaceholders(placeholders)
                .build();
        try {
            notificationService.create(params);
        } catch (Exception e) {
            log.warn("Failed to notify task assignment for {}: {}", task.getTaskId(), e.getMessage());
        }

        OnboardingTaskResponse response = new OnboardingTaskResponse();
        response.setTaskId(task.getTaskId());
        response.setAssigneeUserId(task.getAssignedUserId());
        response.setStatus(task.getStatus());
        return response;
    }

    private static void validate(BizContext context, OnboardingTaskAssignRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getTaskId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "taskId is required");
        }
        if (!StringUtils.hasText(request.getAssigneeUserId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "assigneeUserId is required");
        }
    }

    private static TaskInstanceEntity snapshot(TaskInstanceEntity task) {
        TaskInstanceEntity copy = new TaskInstanceEntity();
        copy.setTaskId(task.getTaskId());
        copy.setCompanyId(task.getCompanyId());
        copy.setStatus(task.getStatus());
        copy.setApprovalStatus(task.getApprovalStatus());
        copy.setAssignedUserId(task.getAssignedUserId());
        return copy;
    }
}
