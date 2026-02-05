package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentAcknowledgeRequest;
import com.sme.be_sme.modules.content.api.response.DocumentAcknowledgeResponse;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAcknowledgementMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAcknowledgementEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskUpdateStatusRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskResponse;
import com.sme.be_sme.modules.onboarding.facade.OnboardingTaskFacade;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class DocumentAcknowledgeProcessor extends BaseBizProcessor<BizContext> {

    private static final String STATUS_ACKED = "ACKED";
    private static final String STATUS_DONE = "DONE";

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentAcknowledgementMapper documentAcknowledgementMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final OnboardingTaskFacade onboardingTaskFacade;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentAcknowledgeRequest request = objectMapper.convertValue(payload, DocumentAcknowledgeRequest.class);
        validate(context, request);

        String companyId = context.getTenantId();
        String userId = context.getOperatorId();
        String documentId = request.getDocumentId().trim();
        String onboardingId = request.getOnboardingId() != null ? request.getOnboardingId().trim() : null;

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        if (!companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "document does not belong to tenant");
        }

        DocumentAcknowledgementEntity existing = null;
        if (StringUtils.hasText(onboardingId)) {
            existing = documentAcknowledgementMapper.selectByCompanyIdAndDocumentIdAndUserIdAndOnboardingId(
                    companyId, documentId, userId, onboardingId);
        }

        String ackId;
        Date now = new Date();
        if (existing != null) {
            ackId = existing.getDocumentAcknowledgementId();
            if (STATUS_ACKED.equalsIgnoreCase(existing.getStatus())) {
                DocumentAcknowledgeResponse response = new DocumentAcknowledgeResponse();
                response.setDocumentAcknowledgementId(ackId);
                response.setDocumentId(documentId);
                response.setOnboardingId(onboardingId);
                response.setTaskMarkedDone(false);
                return response;
            }
            existing.setStatus(STATUS_ACKED);
            existing.setAckedAt(now);
            documentAcknowledgementMapper.updateByPrimaryKey(existing);
        } else {
            ackId = UuidGenerator.generate();
            DocumentAcknowledgementEntity ack = new DocumentAcknowledgementEntity();
            ack.setDocumentAcknowledgementId(ackId);
            ack.setCompanyId(companyId);
            ack.setDocumentId(documentId);
            ack.setUserId(userId);
            ack.setOnboardingId(onboardingId);
            ack.setStatus(STATUS_ACKED);
            ack.setAckedAt(now);
            ack.setCreatedAt(now);
            documentAcknowledgementMapper.insert(ack);
        }

        boolean taskMarkedDone = false;
        if (StringUtils.hasText(onboardingId)) {
            String taskIdToUpdate = request.getTaskId() != null && !request.getTaskId().isBlank()
                    ? request.getTaskId().trim()
                    : findReadHandbookTaskId(companyId, onboardingId, documentId, doc.getTitle());
            if (taskIdToUpdate != null) {
                OnboardingTaskUpdateStatusRequest updateReq = new OnboardingTaskUpdateStatusRequest();
                updateReq.setTaskId(taskIdToUpdate);
                updateReq.setStatus(STATUS_DONE);
                OnboardingTaskResponse taskResp = onboardingTaskFacade.updateTaskStatus(updateReq);
                if (taskResp != null) {
                    taskMarkedDone = true;
                }
            }
        }

        DocumentAcknowledgeResponse response = new DocumentAcknowledgeResponse();
        response.setDocumentAcknowledgementId(ackId);
        response.setDocumentId(documentId);
        response.setOnboardingId(onboardingId);
        response.setTaskMarkedDone(taskMarkedDone);
        return response;
    }

    /**
     * Find a task in the onboarding instance that corresponds to reading this document
     * (e.g. "Read Handbook" or title containing the document title).
     */
    private String findReadHandbookTaskId(String companyId, String onboardingId, String documentId, String documentTitle) {
        List<TaskInstanceEntity> tasks = taskInstanceMapper.selectByCompanyIdAndOnboardingId(companyId, onboardingId);
        if (tasks == null || tasks.isEmpty()) return null;
        String docTitleLower = documentTitle != null ? documentTitle.trim().toLowerCase(Locale.ROOT) : "";
        for (TaskInstanceEntity t : tasks) {
            if (STATUS_DONE.equalsIgnoreCase(t.getStatus())) continue;
            String title = t.getTitle();
            if (title == null) continue;
            String titleLower = title.trim().toLowerCase(Locale.ROOT);
            if ("read handbook".equals(titleLower)
                    || titleLower.contains("read handbook")
                    || (docTitleLower.length() > 0 && titleLower.contains(docTitleLower))) {
                return t.getTaskId();
            }
        }
        return null;
    }

    private static void validate(BizContext context, DocumentAcknowledgeRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId is required");
        }
    }
}
