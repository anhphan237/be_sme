package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.TaskAttachmentAddRequest;
import com.sme.be_sme.modules.onboarding.api.response.TaskAttachmentAddResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskAttachmentMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskAttachmentEntity;
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
public class TaskAttachmentAddProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final TaskAttachmentMapper taskAttachmentMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        TaskAttachmentAddRequest request = objectMapper.convertValue(payload, TaskAttachmentAddRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getTaskId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "taskId is required");
        }
        if (!StringUtils.hasText(request.getFileName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "fileName is required");
        }
        if (!StringUtils.hasText(request.getFileUrl())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "fileUrl is required");
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
                throw AppException.of(ErrorCodes.FORBIDDEN, "only assignee can add attachments to this task");
            }
        }

        Date now = new Date();
        String attachmentId = UuidGenerator.generate();
        TaskAttachmentEntity row = new TaskAttachmentEntity();
        row.setTaskAttachmentId(attachmentId);
        row.setCompanyId(companyId);
        row.setTaskId(task.getTaskId());
        row.setFileName(request.getFileName().trim());
        row.setFileUrl(request.getFileUrl().trim());
        row.setFileType(StringUtils.hasText(request.getFileType()) ? request.getFileType().trim() : null);
        row.setFileSizeBytes(request.getFileSizeBytes());
        row.setUploadedBy(context.getOperatorId());
        row.setUploadedAt(now);

        if (taskAttachmentMapper.insert(row) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "save attachment failed");
        }

        TaskAttachmentAddResponse response = new TaskAttachmentAddResponse();
        response.setAttachmentId(attachmentId);
        return response;
    }
}
