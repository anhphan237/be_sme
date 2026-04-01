package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.TaskScheduleCancelRequest;
import com.sme.be_sme.modules.onboarding.api.response.TaskScheduleResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskActivityLogService;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskWorkflowNotificationService;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskAuth;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskWorkflow;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class TaskScheduleCancelProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final OnboardingTaskActivityLogService activityLogService;
    private final OnboardingTaskWorkflowNotificationService workflowNotificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        TaskScheduleCancelRequest request = objectMapper.convertValue(payload, TaskScheduleCancelRequest.class);
        validate(context, request);

        TaskInstanceEntity task = loadTask(context, request.getTaskId());
        enforceAssigneeUnlessElevated(context, task, "only assignee can cancel schedule");
        if (OnboardingTaskWorkflow.STATUS_DONE.equalsIgnoreCase(task.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "cannot cancel schedule for completed task");
        }

        TaskInstanceEntity before = snapshot(task);
        Date now = new Date();
        task.setScheduleStatus(OnboardingTaskWorkflow.SCHEDULE_CANCELLED);
        task.setScheduleCancelReason(request.getReason().trim());
        task.setScheduleNoShowReason(null);
        task.setScheduleRescheduleReason(null);
        task.setScheduleConfirmedBy(null);
        task.setScheduleConfirmedAt(null);
        task.setUpdatedAt(now);

        if (taskInstanceMapper.updateByPrimaryKey(task) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "cancel task schedule failed");
        }
        activityLogService.logScheduleCancelled(before, task, context.getOperatorId());
        workflowNotificationService.notifyScheduleCancelled(task, context.getTenantId(), context.getOperatorId());
        return toResponse(task);
    }

    private static void validate(BizContext context, TaskScheduleCancelRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getTaskId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "taskId is required");
        }
        if (!StringUtils.hasText(request.getReason())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "reason is required");
        }
    }

    private static void enforceAssigneeUnlessElevated(BizContext context, TaskInstanceEntity task, String message) {
        if (OnboardingTaskAuth.isHrManagerAdmin(context.getRoles())) {
            return;
        }
        if (!StringUtils.hasText(task.getAssignedUserId())
                || !task.getAssignedUserId().equals(context.getOperatorId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, message);
        }
    }

    private TaskInstanceEntity loadTask(BizContext context, String taskId) {
        TaskInstanceEntity task = taskInstanceMapper.selectByPrimaryKey(taskId.trim());
        if (task == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "task not found");
        }
        if (!context.getTenantId().equals(task.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "task does not belong to tenant");
        }
        return task;
    }

    private static TaskScheduleResponse toResponse(TaskInstanceEntity task) {
        TaskScheduleResponse response = new TaskScheduleResponse();
        response.setTaskId(task.getTaskId());
        response.setScheduleStatus(task.getScheduleStatus());
        response.setScheduledStartAt(task.getScheduledStartAt());
        response.setScheduledEndAt(task.getScheduledEndAt());
        response.setScheduleProposedBy(task.getScheduleProposedBy());
        response.setScheduleProposedAt(task.getScheduleProposedAt());
        response.setScheduleConfirmedBy(task.getScheduleConfirmedBy());
        response.setScheduleConfirmedAt(task.getScheduleConfirmedAt());
        response.setScheduleRescheduleReason(task.getScheduleRescheduleReason());
        response.setScheduleCancelReason(task.getScheduleCancelReason());
        response.setScheduleNoShowReason(task.getScheduleNoShowReason());
        return response;
    }

    private static TaskInstanceEntity snapshot(TaskInstanceEntity task) {
        TaskInstanceEntity copy = new TaskInstanceEntity();
        copy.setTaskId(task.getTaskId());
        copy.setCompanyId(task.getCompanyId());
        copy.setScheduleStatus(task.getScheduleStatus());
        copy.setScheduleCancelReason(task.getScheduleCancelReason());
        return copy;
    }
}

