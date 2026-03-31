package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskRejectRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.service.OnboardingInstanceProgressService;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskApprovalAuthority;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskWorkflow;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingTaskRejectProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final OnboardingInstanceProgressService progressService;
    private final OnboardingTaskApprovalAuthority approvalAuthority;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingTaskRejectRequest request = objectMapper.convertValue(payload, OnboardingTaskRejectRequest.class);
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

        Date now = new Date();
        task.setStatus("TODO");
        task.setApprovalStatus(OnboardingTaskWorkflow.APPROVAL_REJECTED);
        task.setRejectionReason(StringUtils.hasText(request.getReason()) ? request.getReason().trim() : null);
        task.setCompletedAt(null);
        task.setApprovedBy(null);
        task.setApprovedAt(null);
        task.setUpdatedAt(now);

        if (taskInstanceMapper.updateByPrimaryKey(task) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "reject task failed");
        }

        progressService.recalculateFromTask(companyId, task);

        OnboardingTaskResponse response = new OnboardingTaskResponse();
        response.setTaskId(task.getTaskId());
        response.setAssigneeUserId(task.getAssignedUserId());
        response.setStatus(task.getStatus());
        return response;
    }
}
