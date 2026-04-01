package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskApproveRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskActivityLogService;
import com.sme.be_sme.modules.onboarding.service.OnboardingInstanceProgressService;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskApprovalAuthority;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskWorkflowNotificationService;
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
public class OnboardingTaskApproveProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final OnboardingInstanceProgressService progressService;
    private final OnboardingTaskApprovalAuthority approvalAuthority;
    private final OnboardingTaskActivityLogService activityLogService;
    private final OnboardingTaskWorkflowNotificationService workflowNotificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingTaskApproveRequest request = objectMapper.convertValue(payload, OnboardingTaskApproveRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getTaskId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "taskId is required");
        }
        String companyId = context.getTenantId();
        TaskInstanceEntity task = taskInstanceMapper.selectByPrimaryKey(request.getTaskId().trim());
        if (task == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "task not found");
        }
        if (!companyId.equals(task.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "task does not belong to tenant");
        }
        if (!Boolean.TRUE.equals(task.getRequiresManagerApproval())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "task does not require manager approval");
        }
        approvalAuthority.assertMayApproveOrRejectOrForceDone(context, companyId, task);
        if (!OnboardingTaskWorkflow.STATUS_PENDING_APPROVAL.equalsIgnoreCase(task.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "task is not pending approval");
        }
        if (!OnboardingTaskWorkflow.APPROVAL_PENDING.equalsIgnoreCase(
                task.getApprovalStatus() == null ? "" : task.getApprovalStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "task is not pending approval");
        }

        TaskInstanceEntity before = snapshot(task);
        Date now = new Date();
        task.setStatus(OnboardingTaskWorkflow.STATUS_DONE);
        task.setApprovalStatus(OnboardingTaskWorkflow.APPROVAL_APPROVED);
        task.setApprovedBy(context.getOperatorId());
        task.setApprovedAt(now);
        task.setCompletedAt(now);
        task.setRejectionReason(null);
        task.setUpdatedAt(now);

        if (taskInstanceMapper.updateByPrimaryKey(task) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "approve task failed");
        }
        activityLogService.logApproved(before, task, context.getOperatorId());
        activityLogService.logStatusChanged(before, task, context.getOperatorId());
        workflowNotificationService.notifyApproved(task, companyId);

        progressService.recalculateFromTask(companyId, task);

        OnboardingTaskResponse response = new OnboardingTaskResponse();
        response.setTaskId(task.getTaskId());
        response.setAssigneeUserId(task.getAssignedUserId());
        response.setStatus(task.getStatus());
        return response;
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
