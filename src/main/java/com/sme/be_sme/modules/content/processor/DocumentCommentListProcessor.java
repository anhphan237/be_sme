package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentCommentListRequest;
import com.sme.be_sme.modules.content.api.response.DocumentCommentListResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentCommentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentCommentEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DocumentCommentListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentCommentMapper documentCommentMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentCommentListRequest request = objectMapper.convertValue(payload, DocumentCommentListRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId is required");
        }

        String companyId = context.getTenantId();
        String documentId = request.getDocumentId().trim();
        int limit = request.getLimit() != null && request.getLimit() > 0
                ? Math.min(request.getLimit(), 500) : 200;

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        List<DocumentCommentEntity> rows = documentCommentMapper
                .selectByCompanyIdAndDocumentIdOrderByCreatedAsc(companyId, documentId, limit);
        List<DocumentCommentListResponse.CommentRow> items = new ArrayList<>();
        if (rows != null) {
            for (DocumentCommentEntity c : rows) {
                DocumentCommentListResponse.CommentRow item = new DocumentCommentListResponse.CommentRow();
                item.setCommentId(c.getDocumentCommentId());
                item.setParentCommentId(c.getParentCommentId());
                item.setAuthorUserId(c.getAuthorUserId());
                item.setBody(c.getBody());
                item.setStatus(c.getStatus());
                item.setCreatedAt(c.getCreatedAt());
                item.setUpdatedAt(c.getUpdatedAt());
                items.add(item);
            }
        }

        DocumentCommentListResponse response = new DocumentCommentListResponse();
        response.setDocumentId(documentId);
        response.setItems(items);
        return response;
    }
}
