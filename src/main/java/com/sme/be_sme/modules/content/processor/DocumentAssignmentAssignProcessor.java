package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentAssignmentAssignRequest;
import com.sme.be_sme.modules.content.api.response.DocumentAssignmentAssignResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentActivityLogMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAssignmentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentActivityLogEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAssignmentEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentAssignmentAssignProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentAssignmentMapper documentAssignmentMapper;
    private final DocumentActivityLogMapper documentActivityLogMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;
    private final NotificationService notificationService;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentAssignmentAssignRequest request = objectMapper.convertValue(payload, DocumentAssignmentAssignRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId()) || !StringUtils.hasText(request.getAssigneeUserId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId and assigneeUserId are required");
        }

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        String documentId = request.getDocumentId().trim();
        String assigneeId = request.getAssigneeUserId().trim();

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        Date now = new Date();
        DocumentAssignmentEntity latest = documentAssignmentMapper.selectLatestByCompanyDocumentAndAssignee(
                companyId, documentId, assigneeId);

        String assignmentId;
        boolean notifyAssignee = false;
        if (latest != null && DocumentEditorConstants.ASSIGNMENT_STATUS_ASSIGNED.equalsIgnoreCase(
                norm(latest.getStatus()))) {
            assignmentId = latest.getDocumentAssignmentId();
        } else if (latest != null && DocumentEditorConstants.ASSIGNMENT_STATUS_REVOKED.equalsIgnoreCase(
                norm(latest.getStatus()))) {
            latest.setStatus(DocumentEditorConstants.ASSIGNMENT_STATUS_ASSIGNED);
            latest.setAssignedByUserId(operatorId);
            latest.setUpdatedAt(now);
            if (documentAssignmentMapper.updateByPrimaryKey(latest) != 1) {
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to reactivate assignment");
            }
            assignmentId = latest.getDocumentAssignmentId();
            logAssigned(companyId, documentId, operatorId, assignmentId, assigneeId, now);
            notifyAssignee = true;
        } else {
            assignmentId = UuidGenerator.generate();
            DocumentAssignmentEntity row = new DocumentAssignmentEntity();
            row.setDocumentAssignmentId(assignmentId);
            row.setCompanyId(companyId);
            row.setDocumentId(documentId);
            row.setAssigneeUserId(assigneeId);
            row.setAssignedByUserId(operatorId);
            row.setStatus(DocumentEditorConstants.ASSIGNMENT_STATUS_ASSIGNED);
            row.setAssignedAt(now);
            row.setUpdatedAt(now);
            if (documentAssignmentMapper.insert(row) != 1) {
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to assign");
            }
            logAssigned(companyId, documentId, operatorId, assignmentId, assigneeId, now);
            notifyAssignee = true;
        }

        if (notifyAssignee) {
            notifyAssignment(companyId, assigneeId, documentId, doc);
        }

        DocumentAssignmentAssignResponse response = new DocumentAssignmentAssignResponse();
        response.setDocumentAssignmentId(assignmentId);
        response.setDocumentId(documentId);
        response.setAssigneeUserId(assigneeId);
        response.setStatus(DocumentEditorConstants.ASSIGNMENT_STATUS_ASSIGNED);
        return response;
    }

    private void logAssigned(String companyId, String documentId, String operatorId, String assignmentId,
                             String assigneeId, Date now) {
        DocumentActivityLogEntity log = new DocumentActivityLogEntity();
        log.setDocumentActivityLogId(UuidGenerator.generate());
        log.setCompanyId(companyId);
        log.setDocumentId(documentId);
        log.setAction(DocumentEditorConstants.ACTION_ASSIGNMENT_ASSIGNED);
        log.setActorUserId(operatorId);
        log.setDetailJson("{\"documentAssignmentId\":\"" + assignmentId + "\",\"assigneeUserId\":\"" + assigneeId + "\"}");
        log.setCreatedAt(now);
        documentActivityLogMapper.insert(log);
    }

    private static String norm(String s) {
        return s != null ? s.trim() : "";
    }

    private void notifyAssignment(String companyId, String assigneeUserId, String documentId, DocumentEntity doc) {
        String title = StringUtils.hasText(doc.getTitle()) ? doc.getTitle().trim() : "Document";
        try {
            notificationService.create(NotificationCreateParams.builder()
                    .companyId(companyId)
                    .userId(assigneeUserId)
                    .type(DocumentEditorConstants.NOTIFICATION_TYPE_DOCUMENT_ASSIGNED)
                    .title("Assigned to document: " + title)
                    .content("You have been assigned to a document.")
                    .refType(DocumentEditorConstants.NOTIFICATION_REF_TYPE_DOCUMENT)
                    .refId(documentId)
                    .sendEmail(false)
                    .build());
        } catch (Exception e) {
            log.warn("Document assignment notification failed for documentId={} assignee={}: {}",
                    documentId, assigneeUserId, e.getMessage());
        }
    }
}
