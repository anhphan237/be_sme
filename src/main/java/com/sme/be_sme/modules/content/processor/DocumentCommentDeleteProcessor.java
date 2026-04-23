package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentCommentDeleteRequest;
import com.sme.be_sme.modules.content.api.response.DocumentCommentDeleteResponse;
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
public class DocumentCommentDeleteProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentCommentMapper documentCommentMapper;
    private final DocumentActivityLogMapper documentActivityLogMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentCommentDeleteRequest request = objectMapper.convertValue(payload, DocumentCommentDeleteRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getCommentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "commentId is required");
        }

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        String commentId = request.getCommentId().trim();

        DocumentCommentEntity comment = documentCommentMapper.selectByPrimaryKey(commentId);
        if (comment == null || !companyId.equals(comment.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "comment not found");
        }

        DocumentEntity doc = documentMapper.selectByPrimaryKey(comment.getDocumentId());
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        boolean author = StringUtils.hasText(operatorId) && operatorId.equals(comment.getAuthorUserId());
        if (!author && !documentAccessEvaluator.isManagementBypass(context.getRoles())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "cannot delete this comment");
        }

        if (DocumentEditorConstants.STATUS_DELETED.equalsIgnoreCase(
                comment.getStatus() != null ? comment.getStatus().trim() : "")) {
            DocumentCommentDeleteResponse already = new DocumentCommentDeleteResponse();
            already.setDeleted(false);
            already.setDocumentId(comment.getDocumentId());
            return already;
        }

        Date now = new Date();
        comment.setStatus(DocumentEditorConstants.STATUS_DELETED);
        comment.setUpdatedAt(now);
        if (documentCommentMapper.updateByPrimaryKey(comment) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to delete comment");
        }

        DocumentActivityLogEntity log = new DocumentActivityLogEntity();
        log.setDocumentActivityLogId(UuidGenerator.generate());
        log.setCompanyId(companyId);
        log.setDocumentId(comment.getDocumentId());
        log.setAction(DocumentEditorConstants.ACTION_COMMENT_DELETE);
        log.setActorUserId(operatorId);
        log.setDetailJson("{\"commentId\":\"" + commentId + "\"}");
        log.setCreatedAt(now);
        documentActivityLogMapper.insert(log);

        DocumentCommentDeleteResponse response = new DocumentCommentDeleteResponse();
        response.setDeleted(true);
        response.setDocumentId(comment.getDocumentId());
        return response;
    }
}
