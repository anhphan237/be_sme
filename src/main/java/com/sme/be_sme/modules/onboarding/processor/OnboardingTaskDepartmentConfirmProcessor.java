package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.infrastructure.mapper.DepartmentMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentEntity;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskDepartmentConfirmRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskDepartmentConfirmResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskDepartmentCheckpointMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskDepartmentCheckpointEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.service.OnboardingInstanceProgressService;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskActivityLogService;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskWorkflow;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
public class OnboardingTaskDepartmentConfirmProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final TaskDepartmentCheckpointMapper taskDepartmentCheckpointMapper;
    private final DepartmentMapper departmentMapper;
    private final OnboardingTaskActivityLogService activityLogService;
    private final OnboardingInstanceProgressService progressService;

    public OnboardingTaskDepartmentConfirmProcessor(
            ObjectMapper objectMapper,
            TaskInstanceMapper taskInstanceMapper,
            TaskDepartmentCheckpointMapper taskDepartmentCheckpointMapper,
            DepartmentMapper departmentMapper,
            OnboardingTaskActivityLogService activityLogService,
            OnboardingInstanceProgressService progressService) {
        this.objectMapper = objectMapper;
        this.taskInstanceMapper = taskInstanceMapper;
        this.taskDepartmentCheckpointMapper = taskDepartmentCheckpointMapper;
        this.departmentMapper = departmentMapper;
        this.activityLogService = activityLogService;
        this.progressService = progressService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingTaskDepartmentConfirmRequest request =
                objectMapper.convertValue(payload, OnboardingTaskDepartmentConfirmRequest.class);
        validate(context, request);

        String companyId = context.getTenantId().trim();
        String operatorId = context.getOperatorId().trim();
        String taskId = request.getTaskId().trim();
        String departmentId = request.getDepartmentId().trim();

        TaskInstanceEntity task = taskInstanceMapper.selectByPrimaryKey(taskId);
        if (task == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "task not found");
        }
        if (!companyId.equals(task.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "task does not belong to tenant");
        }

        TaskDepartmentCheckpointEntity checkpoint = taskDepartmentCheckpointMapper
                .selectByCompanyIdAndTaskIdAndDepartmentId(companyId, taskId, departmentId);
        if (checkpoint == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "department checkpoint not configured for this task");
        }

        DepartmentEntity department = departmentMapper.selectByPrimaryKey(departmentId);
        if (department == null || !companyId.equals(department.getCompanyId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid department");
        }
        if (!StringUtils.hasText(department.getManagerUserId())
                || !operatorId.equals(department.getManagerUserId().trim())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "only the assigned department manager may confirm");
        }

        String evidenceNote = trimToNull(request.getEvidenceNote());
        String evidenceRef = trimToNull(request.getEvidenceRef());
        if (Boolean.TRUE.equals(checkpoint.getRequireEvidence())
                && !StringUtils.hasText(evidenceNote)
                && !StringUtils.hasText(evidenceRef)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "evidenceNote or evidenceRef is required");
        }

        String oldCheckpointStatus = checkpoint.getStatus();
        Date now = new Date();
        checkpoint.setStatus("CONFIRMED");
        checkpoint.setEvidenceNote(evidenceNote);
        checkpoint.setEvidenceRef(evidenceRef);
        checkpoint.setConfirmedBy(operatorId);
        checkpoint.setConfirmedAt(now);
        checkpoint.setUpdatedAt(now);
        if (taskDepartmentCheckpointMapper.updateByPrimaryKey(checkpoint) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "confirm department checkpoint failed");
        }
        activityLogService.logDepartmentConfirmed(
                task,
                operatorId,
                departmentId,
                oldCheckpointStatus,
                checkpoint.getStatus(),
                evidenceNote,
                evidenceRef);

        TaskInstanceEntity beforeTask = snapshot(task);
        int pendingCount = taskDepartmentCheckpointMapper.countPendingByCompanyIdAndTaskId(companyId, taskId);
        boolean allDepartmentsConfirmed = pendingCount == 0;
        if (allDepartmentsConfirmed && !OnboardingTaskWorkflow.STATUS_DONE.equalsIgnoreCase(task.getStatus())) {
            task.setStatus(OnboardingTaskWorkflow.STATUS_DONE);
            task.setCompletedAt(now);
            if (Boolean.TRUE.equals(task.getRequiresManagerApproval())) {
                task.setApprovalStatus(OnboardingTaskWorkflow.APPROVAL_APPROVED);
                task.setApprovedBy(operatorId);
                task.setApprovedAt(now);
            }
            task.setUpdatedAt(now);
            if (taskInstanceMapper.updateByPrimaryKey(task) != 1) {
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "auto-complete task failed");
            }
            activityLogService.logStatusChanged(beforeTask, task, operatorId);
            progressService.recalculateFromTask(companyId, task);
        }

        OnboardingTaskDepartmentConfirmResponse response = new OnboardingTaskDepartmentConfirmResponse();
        response.setTaskId(taskId);
        response.setDepartmentId(departmentId);
        response.setCheckpointStatus(checkpoint.getStatus());
        response.setTaskStatus(task.getStatus());
        response.setAllDepartmentsConfirmed(allDepartmentsConfirmed);
        return response;
    }

    private static void validate(BizContext context, OnboardingTaskDepartmentConfirmRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (!StringUtils.hasText(context.getOperatorId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "operatorId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getTaskId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "taskId is required");
        }
        if (!StringUtils.hasText(request.getDepartmentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "departmentId is required");
        }
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static TaskInstanceEntity snapshot(TaskInstanceEntity source) {
        TaskInstanceEntity copy = new TaskInstanceEntity();
        copy.setTaskId(source.getTaskId());
        copy.setCompanyId(source.getCompanyId());
        copy.setStatus(source.getStatus());
        copy.setApprovalStatus(source.getApprovalStatus());
        copy.setAssignedUserId(source.getAssignedUserId());
        copy.setCompletedAt(source.getCompletedAt());
        return copy;
    }
}
