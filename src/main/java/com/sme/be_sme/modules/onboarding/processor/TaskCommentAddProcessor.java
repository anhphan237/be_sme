package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.TaskCommentAddRequest;
import com.sme.be_sme.modules.onboarding.api.response.TaskCommentAddResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskCommentMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskCommentEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskAuth;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class TaskCommentAddProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final TaskCommentMapper taskCommentMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        TaskCommentAddRequest request = objectMapper.convertValue(payload, TaskCommentAddRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getTaskId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "taskId is required");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "content is required");
        }

        String companyId = context.getTenantId();
        TaskInstanceEntity task = taskInstanceMapper.selectByPrimaryKey(request.getTaskId().trim());
        if (task == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "task not found");
        }
        if (!companyId.equals(task.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "task does not belong to tenant");
        }

        if (!OnboardingTaskAuth.isHrManagerAdmin(context.getRoles())) {
            if (!StringUtils.hasText(task.getAssignedUserId())
                    || !task.getAssignedUserId().equals(context.getOperatorId())) {
                throw AppException.of(ErrorCodes.FORBIDDEN, "only assignee can add comments to this task");
            }
        }

        TaskCommentEntity row = new TaskCommentEntity();
        String commentId = UuidGenerator.generate();
        row.setTaskCommentId(commentId);
        row.setCompanyId(companyId);
        row.setTaskId(task.getTaskId());
        row.setContent(request.getContent().trim());
        row.setCreatedBy(context.getOperatorId());
        row.setCreatedAt(new Date());
        if (taskCommentMapper.insert(row) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "save comment failed");
        }

        TaskCommentAddResponse response = new TaskCommentAddResponse();
        response.setCommentId(commentId);
        return response;
    }
}
