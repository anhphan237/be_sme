package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentCommentAddRequest;
import com.sme.be_sme.modules.content.api.response.DocumentCommentAddResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentActivityLogMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAssignmentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentCommentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentActivityLogEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAssignmentEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentCommentEntity;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentCommentAddProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentCommentMapper documentCommentMapper;
    private final DocumentActivityLogMapper documentActivityLogMapper;
    private final DocumentAssignmentMapper documentAssignmentMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;
    private final NotificationService notificationService;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentCommentAddRequest request = objectMapper.convertValue(payload, DocumentCommentAddRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId()) || !StringUtils.hasText(request.getBody())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId and body are required");
        }

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        String documentId = request.getDocumentId().trim();
        String body = request.getBody().trim();
        if (body.length() > DocumentEditorConstants.MAX_COMMENT_BODY_CHARS) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "body is too long");
        }

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        String parentId = StringUtils.hasText(request.getParentCommentId()) ? request.getParentCommentId().trim() : null;
        DocumentCommentEntity parentComment = null;
        if (parentId != null) {
            parentComment = documentCommentMapper.selectByPrimaryKey(parentId);
            if (parentComment == null || !companyId.equals(parentComment.getCompanyId())
                    || !documentId.equals(parentComment.getDocumentId())) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "parent comment not found");
            }
        }

        Date now = new Date();
        String commentId = UuidGenerator.generate();
        DocumentCommentEntity row = new DocumentCommentEntity();
        row.setDocumentCommentId(commentId);
        row.setCompanyId(companyId);
        row.setDocumentId(documentId);
        row.setParentCommentId(parentId);
        row.setAnchorBlockId(StringUtils.hasText(request.getAnchorBlockId()) ? request.getAnchorBlockId().trim() : null);
        row.setAnchorStart(request.getAnchorStart());
        row.setAnchorEnd(request.getAnchorEnd());
        row.setAnchorText(StringUtils.hasText(request.getAnchorText()) ? request.getAnchorText().trim() : null);
        row.setAuthorUserId(operatorId);
        row.setBody(body);
        row.setStatus(DocumentEditorConstants.STATUS_ACTIVE);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);

        if (documentCommentMapper.insert(row) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to add comment");
        }

        String action = parentId == null
                ? DocumentEditorConstants.ACTION_COMMENT_ADD
                : DocumentEditorConstants.ACTION_COMMENT_REPLY;
        DocumentActivityLogEntity log = new DocumentActivityLogEntity();
        log.setDocumentActivityLogId(UuidGenerator.generate());
        log.setCompanyId(companyId);
        log.setDocumentId(documentId);
        log.setAction(action);
        log.setActorUserId(operatorId);
        log.setDetailJson("{\"commentId\":\"" + commentId + "\"}");
        log.setCreatedAt(now);
        documentActivityLogMapper.insert(log);

        notifyCommentFollowers(companyId, documentId, operatorId, parentComment, doc, commentId);

        DocumentCommentAddResponse response = new DocumentCommentAddResponse();
        response.setCommentId(commentId);
        response.setDocumentId(documentId);
        response.setParentCommentId(parentId);
        response.setAnchorBlockId(row.getAnchorBlockId());
        response.setAnchorStart(row.getAnchorStart());
        response.setAnchorEnd(row.getAnchorEnd());
        response.setAnchorText(row.getAnchorText());
        response.setCreatedAt(now);
        return response;
    }

    private void notifyCommentFollowers(String companyId, String documentId, String operatorId,
                                        DocumentCommentEntity parentComment, DocumentEntity doc, String commentId) {
        String docTitle = StringUtils.hasText(doc.getTitle()) ? doc.getTitle().trim() : "Document";
        if (parentComment != null) {
            String parentAuthor = parentComment.getAuthorUserId();
            if (StringUtils.hasText(parentAuthor) && !parentAuthor.equals(operatorId)) {
                tryCreateNotification(companyId, parentAuthor, documentId, docTitle, commentId,
                        DocumentEditorConstants.NOTIFICATION_TYPE_DOCUMENT_COMMENT_REPLY,
                        "Reply to your comment on: " + docTitle,
                        "Someone replied to your comment on a document.");
            }
            return;
        }
        List<DocumentAssignmentEntity> assigns =
                documentAssignmentMapper.selectActiveByCompanyAndDocument(companyId, documentId, 500);
        if (assigns == null || assigns.isEmpty()) {
            return;
        }
        Set<String> targets = new LinkedHashSet<>();
        for (DocumentAssignmentEntity a : assigns) {
            if (a != null && StringUtils.hasText(a.getAssigneeUserId())
                    && !a.getAssigneeUserId().equals(operatorId)) {
                targets.add(a.getAssigneeUserId());
            }
        }
        for (String userId : targets) {
            tryCreateNotification(companyId, userId, documentId, docTitle, commentId,
                    DocumentEditorConstants.NOTIFICATION_TYPE_DOCUMENT_COMMENT,
                    "New comment on: " + docTitle,
                    "There is a new comment on a document you are assigned to.");
        }
    }

    private void tryCreateNotification(String companyId, String userId, String documentId, String docTitle,
                                       String commentId, String type, String title, String content) {
        try {
            notificationService.create(NotificationCreateParams.builder()
                    .companyId(companyId)
                    .userId(userId)
                    .type(type)
                    .title(title)
                    .content(content)
                    .refType(DocumentEditorConstants.NOTIFICATION_REF_TYPE_DOCUMENT)
                    .refId(documentId)
                    .sendEmail(false)
                    .build());
        } catch (Exception e) {
            log.warn("Document comment notification failed type={} documentId={} userId={} commentId={}: {}",
                    type, documentId, userId, commentId, e.getMessage());
        }
    }
}
