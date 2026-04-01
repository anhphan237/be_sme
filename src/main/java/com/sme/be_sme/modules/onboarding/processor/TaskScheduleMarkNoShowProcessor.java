package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.TaskScheduleMarkNoShowRequest;
import com.sme.be_sme.modules.onboarding.api.response.TaskScheduleResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskActivityLogService;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskApprovalAuthority;
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
public class TaskScheduleMarkNoShowProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final OnboardingTaskApprovalAuthority approvalAuthority;
    private final OnboardingTaskActivityLogService activityLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        TaskScheduleMarkNoShowRequest request =
                objectMapper.convertValue(payload, TaskScheduleMarkNoShowRequest.class);
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
            throw AppException.of(ErrorCodes.BAD_REQUEST, "task already completed");
        }
        if (task.getScheduledStartAt() == null || task.getScheduledStartAt().after(new Date())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "task schedule has not started yet");
        }

        assertMayMarkNoShow(context, companyId, task);

        TaskInstanceEntity before = snapshot(task);
        Date now = new Date();
        task.setScheduleStatus(OnboardingTaskWorkflow.SCHEDULE_MISSED);
        task.setScheduleNoShowReason(request.getReason().trim());
        task.setUpdatedAt(now);

        if (taskInstanceMapper.updateByPrimaryKey(task) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "mark no-show failed");
        }
        activityLogService.logScheduleNoShow(before, task, context.getOperatorId());
        return toResponse(task);
    }

    private void assertMayMarkNoShow(BizContext context, String companyId, TaskInstanceEntity task) {
        if (OnboardingTaskAuth.isHrManagerAdmin(context.getRoles())) {
            return;
        }
        String operatorId = context.getOperatorId();
        if (!StringUtils.hasText(operatorId)) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "operator required");
        }
        if (StringUtils.hasText(task.getAssignedUserId()) && operatorId.equals(task.getAssignedUserId())) {
            return;
        }
        String expectedConfirmer = null;
        if (StringUtils.hasText(task.getApproverUserId())) {
            expectedConfirmer = task.getApproverUserId().trim();
        } else {
            OnboardingInstanceEntity instance = approvalAuthority.loadOnboardingInstance(companyId, task);
            expectedConfirmer = approvalAuthority.resolveLineManagerUserId(companyId, instance);
        }
        if (!StringUtils.hasText(expectedConfirmer) || !expectedConfirmer.equals(operatorId.trim())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "only related actors can mark no-show");
        }
    }

    private static void validate(BizContext context, TaskScheduleMarkNoShowRequest request) {
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
        copy.setScheduleNoShowReason(task.getScheduleNoShowReason());
        return copy;
    }
}

