package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentCommentUpdateRequest;
import com.sme.be_sme.modules.content.api.response.DocumentCommentUpdateResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentActivityLogMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentCommentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentActivityLogEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentCommentEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class DocumentCommentUpdateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentCommentMapper documentCommentMapper;
    private final DocumentActivityLogMapper documentActivityLogMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentCommentUpdateRequest request = objectMapper.convertValue(payload, DocumentCommentUpdateRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getCommentId()) || !StringUtils.hasText(request.getBody())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "commentId and body are required");
        }

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        String commentId = request.getCommentId().trim();
        String body = request.getBody().trim();
        if (body.length() > DocumentEditorConstants.MAX_COMMENT_BODY_CHARS) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "body is too long");
        }

        DocumentCommentEntity comment = documentCommentMapper.selectByPrimaryKey(commentId);
        if (comment == null || !companyId.equals(comment.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "comment not found");
        }

        if (DocumentEditorConstants.STATUS_DELETED.equalsIgnoreCase(
                comment.getStatus() != null ? comment.getStatus().trim() : "")) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "comment is deleted");
        }

        DocumentEntity doc = documentMapper.selectByPrimaryKey(comment.getDocumentId());
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        boolean author = StringUtils.hasText(operatorId) && operatorId.equals(comment.getAuthorUserId());
        if (!author && !documentAccessEvaluator.isManagementBypass(context.getRoles())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "cannot update this comment");
        }

        Date now = new Date();
        comment.setBody(body);
        comment.setUpdatedAt(now);
        if (documentCommentMapper.updateByPrimaryKey(comment) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to update comment");
        }

        DocumentActivityLogEntity log = new DocumentActivityLogEntity();
        log.setDocumentActivityLogId(UuidGenerator.generate());
        log.setCompanyId(companyId);
        log.setDocumentId(comment.getDocumentId());
        log.setAction(DocumentEditorConstants.ACTION_COMMENT_UPDATE);
        log.setActorUserId(operatorId);
        log.setDetailJson("{\"commentId\":\"" + commentId + "\"}");
        log.setCreatedAt(now);
        documentActivityLogMapper.insert(log);

        DocumentCommentUpdateResponse response = new DocumentCommentUpdateResponse();
        response.setCommentId(commentId);
        response.setDocumentId(comment.getDocumentId());
        response.setUpdatedAt(now);
        return response;
    }
}
