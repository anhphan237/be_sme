package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskUpdateStatusRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.ChecklistInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.ChecklistInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingTaskUpdateStatusProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final ChecklistInstanceMapper checklistInstanceMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;

    @Override
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

        String newStatus = request.getStatus().trim();
        task.setStatus(newStatus);
        if ("DONE".equalsIgnoreCase(newStatus)) {
            task.setCompletedAt(new Date());
        }
        task.setUpdatedAt(new Date());

        int updated = taskInstanceMapper.updateByPrimaryKey(task);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "update task status failed");
        }

        updateParentInstanceProgress(companyId, task);
        if (context.getOperatorId() != null) {
            // Audit: status change could be logged to task_activity_logs here if needed
        }

        OnboardingTaskResponse response = new OnboardingTaskResponse();
        response.setTaskId(task.getTaskId());
        response.setAssigneeUserId(task.getAssignedUserId());
        response.setStatus(task.getStatus());
        return response;
    }

    private void updateParentInstanceProgress(String companyId, TaskInstanceEntity task) {
        ChecklistInstanceEntity checklist = checklistInstanceMapper.selectByPrimaryKey(task.getChecklistId());
        if (checklist == null || !companyId.equals(checklist.getCompanyId())) {
            return;
        }
        String onboardingId = checklist.getOnboardingId();
        if (onboardingId == null) {
            return;
        }
        OnboardingInstanceEntity instance = onboardingInstanceMapper.selectByPrimaryKey(onboardingId);
        if (instance == null || !companyId.equals(instance.getCompanyId())) {
            return;
        }
        List<TaskInstanceEntity> tasks = taskInstanceMapper.selectByCompanyIdAndOnboardingId(companyId, onboardingId);
        int total = tasks == null ? 0 : tasks.size();
        if (total == 0) {
            return;
        }
        long doneCount = tasks.stream().filter(t -> "DONE".equalsIgnoreCase(t.getStatus())).count();
        int progressPercent = (int) ((doneCount * 100) / total);
        instance.setProgressPercent(progressPercent);
        instance.setUpdatedAt(new Date());
        onboardingInstanceMapper.updateByPrimaryKey(instance);
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
