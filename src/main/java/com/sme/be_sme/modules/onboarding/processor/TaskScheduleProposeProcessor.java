package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.TaskScheduleProposeRequest;
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
public class TaskScheduleProposeProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final OnboardingTaskActivityLogService activityLogService;
    private final OnboardingTaskWorkflowNotificationService workflowNotificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        TaskScheduleProposeRequest request = objectMapper.convertValue(payload, TaskScheduleProposeRequest.class);
        validate(context, request);

        String companyId = context.getTenantId().trim();
        TaskInstanceEntity task = taskInstanceMapper.selectByPrimaryKey(request.getTaskId().trim());
        if (task == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "task not found");
        }
        if (!companyId.equals(task.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "task does not belong to tenant");
        }
        if (OnboardingTaskWorkflow.STATUS_DONE.equalsIgnoreCase(task.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "cannot schedule a completed task");
        }
        enforceAssigneeUnlessElevated(context, task);

        Date start = request.getScheduledStartAt();
        Date end = request.getScheduledEndAt();
        if (end != null && end.before(start)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "scheduledEndAt must be after scheduledStartAt");
        }

        TaskInstanceEntity before = snapshot(task);
        Date now = new Date();
        task.setScheduledStartAt(start);
        task.setScheduledEndAt(end);
        task.setScheduleStatus(OnboardingTaskWorkflow.SCHEDULE_PROPOSED);
        task.setScheduleProposedBy(context.getOperatorId());
        task.setScheduleProposedAt(now);
        task.setScheduleConfirmedBy(null);
        task.setScheduleConfirmedAt(null);
        task.setScheduleRescheduleReason(null);
        task.setScheduleCancelReason(null);
        task.setScheduleNoShowReason(null);
        task.setUpdatedAt(now);

        if (taskInstanceMapper.updateByPrimaryKey(task) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "propose task schedule failed");
        }
        activityLogService.logScheduleProposed(before, task, context.getOperatorId());
        workflowNotificationService.notifyScheduleProposed(task, companyId, context.getOperatorId());
        return toResponse(task);
    }

    private static void enforceAssigneeUnlessElevated(BizContext context, TaskInstanceEntity task) {
        if (OnboardingTaskAuth.isHrManagerAdmin(context.getRoles())) {
            return;
        }
        if (!StringUtils.hasText(task.getAssignedUserId())
                || !task.getAssignedUserId().equals(context.getOperatorId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "only assignee can propose schedule");
        }
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

    private static void validate(BizContext context, TaskScheduleProposeRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getTaskId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "taskId is required");
        }
        if (request.getScheduledStartAt() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "scheduledStartAt is required");
        }
    }

    private static TaskInstanceEntity snapshot(TaskInstanceEntity task) {
        TaskInstanceEntity copy = new TaskInstanceEntity();
        copy.setTaskId(task.getTaskId());
        copy.setCompanyId(task.getCompanyId());
        copy.setScheduleStatus(task.getScheduleStatus());
        copy.setScheduledStartAt(task.getScheduledStartAt());
        copy.setScheduledEndAt(task.getScheduledEndAt());
        copy.setScheduleProposedBy(task.getScheduleProposedBy());
        copy.setScheduleProposedAt(task.getScheduleProposedAt());
        copy.setScheduleConfirmedBy(task.getScheduleConfirmedBy());
        copy.setScheduleConfirmedAt(task.getScheduleConfirmedAt());
        copy.setScheduleRescheduleReason(task.getScheduleRescheduleReason());
        copy.setScheduleCancelReason(task.getScheduleCancelReason());
        copy.setScheduleNoShowReason(task.getScheduleNoShowReason());
        return copy;
    }
}

