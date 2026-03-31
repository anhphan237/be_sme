package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.OnboardTaskEnum;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskAcknowledgeRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskAuth;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class OnboardingTaskAcknowledgeProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final TaskInstanceMapper taskInstanceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingTaskAcknowledgeRequest request =
                objectMapper.convertValue(payload, OnboardingTaskAcknowledgeRequest.class);
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
        if (!Boolean.TRUE.equals(task.getRequireAck())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "task does not require acknowledgement");
        }

        if (!OnboardingTaskAuth.isHrManagerAdmin(context.getRoles())) {
            if (!StringUtils.hasText(task.getAssignedUserId())
                    || !task.getAssignedUserId().equals(context.getOperatorId())) {
                throw AppException.of(ErrorCodes.FORBIDDEN, "only assignee can acknowledge this task");
            }
        }

        Date now = new Date();
        task.setAcknowledgedAt(now);
        task.setAcknowledgedBy(context.getOperatorId());
        task.setUpdatedAt(now);
        task.setStatus(OnboardTaskEnum.DONE.toString());
        if (taskInstanceMapper.updateByPrimaryKey(task) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "acknowledge task failed");
        }

        OnboardingTaskResponse response = new OnboardingTaskResponse();
        response.setTaskId(task.getTaskId());
        response.setAssigneeUserId(task.getAssignedUserId());
        response.setStatus(task.getStatus());
        return response;
    }
}
