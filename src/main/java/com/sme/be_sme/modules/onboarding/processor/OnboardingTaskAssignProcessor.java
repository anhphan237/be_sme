package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskAssignRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
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
public class OnboardingTaskAssignProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final TaskInstanceMapper taskInstanceMapper;

    @Override
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

        task.setAssignedUserId(request.getAssigneeUserId().trim());
        task.setStatus("ASSIGNED");
        task.setUpdatedAt(new Date());

        int updated = taskInstanceMapper.updateByPrimaryKey(task);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "assign task failed");
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
}
