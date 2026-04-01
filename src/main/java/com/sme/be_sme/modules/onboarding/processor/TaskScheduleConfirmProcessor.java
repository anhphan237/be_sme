package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.TaskScheduleConfirmRequest;
import com.sme.be_sme.modules.onboarding.api.response.TaskScheduleResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskActivityLogService;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskApprovalAuthority;
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
public class TaskScheduleConfirmProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final OnboardingTaskApprovalAuthority approvalAuthority;
    private final OnboardingTaskActivityLogService activityLogService;
    private final OnboardingTaskWorkflowNotificationService workflowNotificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        TaskScheduleConfirmRequest request = objectMapper.convertValue(payload, TaskScheduleConfirmRequest.class);
        validate(context, request);

        String companyId = context.getTenantId().trim();
        TaskInstanceEntity task = taskInstanceMapper.selectByPrimaryKey(request.getTaskId().trim());
        if (task == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "task not found");
        }
        if (!companyId.equals(task.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "task does not belong to tenant");
        }
        if (!OnboardingTaskWorkflow.SCHEDULE_PROPOSED.equalsIgnoreCase(task.getScheduleStatus())
                && !OnboardingTaskWorkflow.SCHEDULE_RESCHEDULED.equalsIgnoreCase(task.getScheduleStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "task schedule is not awaiting confirmation");
        }

        assertMayConfirm(context, companyId, task);

        if (StringUtils.hasText(task.getScheduleProposedBy())
                && task.getScheduleProposedBy().equals(context.getOperatorId())
                && !OnboardingTaskAuth.isHrManagerAdmin(context.getRoles())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "proposer cannot self-confirm schedule");
        }

        TaskInstanceEntity before = snapshot(task);
        Date now = new Date();
        task.setScheduleStatus(OnboardingTaskWorkflow.SCHEDULE_CONFIRMED);
        task.setScheduleConfirmedBy(context.getOperatorId());
        task.setScheduleConfirmedAt(now);
        task.setUpdatedAt(now);

        if (taskInstanceMapper.updateByPrimaryKey(task) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "confirm task schedule failed");
        }
        activityLogService.logScheduleConfirmed(before, task, context.getOperatorId());
        workflowNotificationService.notifyScheduleConfirmed(task, companyId, context.getOperatorId());
        return toResponse(task);
    }

    private void assertMayConfirm(BizContext context, String companyId, TaskInstanceEntity task) {
        if (OnboardingTaskAuth.isHrManagerAdmin(context.getRoles())) {
            return;
        }
        String operator = context.getOperatorId();
        if (!StringUtils.hasText(operator)) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "operator required");
        }
        String expectedConfirmer = null;
        if (StringUtils.hasText(task.getApproverUserId())) {
            expectedConfirmer = task.getApproverUserId().trim();
        } else {
            OnboardingInstanceEntity instance = approvalAuthority.loadOnboardingInstance(companyId, task);
            expectedConfirmer = approvalAuthority.resolveLineManagerUserId(companyId, instance);
        }
        if (!StringUtils.hasText(expectedConfirmer)) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "no confirmer configured (set approverUserId or onboarding line manager)");
        }
        if (!expectedConfirmer.equals(operator.trim())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "only configured confirmer can confirm schedule");
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

    private static void validate(BizContext context, TaskScheduleConfirmRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getTaskId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "taskId is required");
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

