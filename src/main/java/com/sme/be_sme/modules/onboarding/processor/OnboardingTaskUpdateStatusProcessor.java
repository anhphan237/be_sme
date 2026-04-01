package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskUpdateStatusRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.service.OnboardingInstanceProgressService;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskApprovalAuthority;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskAuth;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskWorkflow;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class OnboardingTaskUpdateStatusProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final OnboardingInstanceProgressService progressService;
    private final OnboardingTaskApprovalAuthority approvalAuthority;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingTaskUpdateStatusRequest request = objectMapper.convertValue(payload, OnboardingTaskUpdateStatusRequest.class);
        validate(context, request);

        String companyId = context.getTenantId();
        TaskInstanceEntity task = taskInstanceMapper.selectByPrimaryKey(request.getTaskId().trim());
        if (task == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "task not found");
        }
        if (!companyId.equals(task.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "task does not belong to tenant");
        }

        enforceAssigneeUnlessElevated(context, task);

        String newStatus = OnboardingTaskWorkflow.normalizeStatus(request.getStatus());
        if (!OnboardingTaskWorkflow.isKnownStatus(newStatus)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid status value");
        }
        String currentStatus = OnboardingTaskWorkflow.normalizeStatus(task.getStatus());
        if (!OnboardingTaskWorkflow.canTransition(currentStatus, newStatus)) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "invalid status transition from " + currentStatus + " to " + newStatus);
        }
        Date now = new Date();

        if (OnboardingTaskWorkflow.STATUS_PENDING_APPROVAL.equalsIgnoreCase(newStatus)) {
            if (!Boolean.TRUE.equals(task.getRequiresManagerApproval())) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "task does not require manager approval");
            }
            task.setStatus(OnboardingTaskWorkflow.STATUS_PENDING_APPROVAL);
            task.setApprovalStatus(OnboardingTaskWorkflow.APPROVAL_PENDING);
            task.setCompletedAt(null);
            task.setApprovedBy(null);
            task.setApprovedAt(null);
            task.setRejectionReason(null);
        } else if (OnboardingTaskWorkflow.STATUS_DONE.equalsIgnoreCase(newStatus)) {
            if (Boolean.TRUE.equals(task.getRequireAck()) && task.getAcknowledgedAt() == null) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "acknowledge task before marking done");
            }
            if (Boolean.TRUE.equals(task.getRequiresManagerApproval())) {
                if (OnboardingTaskAuth.isEmployeeOnly(context.getRoles())) {
                    throw AppException.of(
                            ErrorCodes.BAD_REQUEST,
                            "use status " + OnboardingTaskWorkflow.STATUS_PENDING_APPROVAL + " for manager review");
                }
                approvalAuthority.assertMayApproveOrRejectOrForceDone(context, companyId, task);
                task.setApprovalStatus(OnboardingTaskWorkflow.APPROVAL_APPROVED);
                task.setApprovedBy(context.getOperatorId());
                task.setApprovedAt(now);
            }
            task.setStatus(OnboardingTaskWorkflow.STATUS_DONE);
            task.setCompletedAt(now);
        } else {
            task.setStatus(newStatus);
            if (!OnboardingTaskWorkflow.STATUS_DONE.equalsIgnoreCase(newStatus)) {
                task.setCompletedAt(null);
            }
        }

        task.setUpdatedAt(now);

        int updated = taskInstanceMapper.updateByPrimaryKey(task);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "update task status failed");
        }

        progressService.recalculateFromTask(companyId, task);

        OnboardingTaskResponse response = new OnboardingTaskResponse();
        response.setTaskId(task.getTaskId());
        response.setAssigneeUserId(task.getAssignedUserId());
        response.setStatus(task.getStatus());
        return response;
    }

    private static void enforceAssigneeUnlessElevated(BizContext context, TaskInstanceEntity task) {
        if (OnboardingTaskAuth.isHrManagerAdmin(context.getRoles())) {
            return;
        }
        if (OnboardingTaskAuth.isEmployeeOnly(context.getRoles())) {
            assertAssignee(context, task);
            return;
        }
        if (OnboardingTaskAuth.isItStaffScopedToAssignee(context.getRoles())) {
            assertAssignee(context, task);
        }
    }

    private static void assertAssignee(BizContext context, TaskInstanceEntity task) {
        if (!StringUtils.hasText(task.getAssignedUserId())
                || !task.getAssignedUserId().equals(context.getOperatorId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "only assignee can update this task");
        }
    }

    private static void validate(BizContext context, OnboardingTaskUpdateStatusRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getTaskId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "taskId is required");
        }
        if (!StringUtils.hasText(request.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "status is required");
        }
    }
}
