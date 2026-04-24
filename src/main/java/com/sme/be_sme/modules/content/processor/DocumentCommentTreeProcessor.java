package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentCommentTreeRequest;
import com.sme.be_sme.modules.content.api.response.DocumentCommentTreeResponse;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DocumentCommentTreeProcessor extends BaseBizProcessor<BizContext> {

    private static final Comparator<DocumentCommentTreeResponse.CommentNode> NODE_ORDER =
            Comparator.comparing(DocumentCommentTreeResponse.CommentNode::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(n -> n.getCommentId() != null ? n.getCommentId() : "");

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentCommentMapper documentCommentMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentCommentTreeRequest request = objectMapper.convertValue(payload, DocumentCommentTreeRequest.class);
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
        if (rows == null) {
            rows = new ArrayList<>();
        }

        Map<String, DocumentCommentTreeResponse.CommentNode> byId = new HashMap<>();
        for (DocumentCommentEntity c : rows) {
            DocumentCommentTreeResponse.CommentNode node = new DocumentCommentTreeResponse.CommentNode();
            node.setCommentId(c.getDocumentCommentId());
            node.setParentCommentId(c.getParentCommentId());
            node.setAuthorUserId(c.getAuthorUserId());
            node.setBody(c.getBody());
            node.setStatus(c.getStatus());
            node.setCreatedAt(c.getCreatedAt());
            node.setUpdatedAt(c.getUpdatedAt());
            byId.put(c.getDocumentCommentId(), node);
        }

        List<DocumentCommentTreeResponse.CommentNode> roots = new ArrayList<>();
        for (DocumentCommentEntity c : rows) {
            DocumentCommentTreeResponse.CommentNode node = byId.get(c.getDocumentCommentId());
            String parentId = c.getParentCommentId();
            if (!StringUtils.hasText(parentId) || !byId.containsKey(parentId.trim())) {
                roots.add(node);
            } else {
                byId.get(parentId.trim()).getChildren().add(node);
            }
        }
        sortRecursive(roots);

        DocumentCommentTreeResponse response = new DocumentCommentTreeResponse();
        response.setDocumentId(documentId);
        response.setRoots(roots);
        return response;
    }

    private static void sortRecursive(List<DocumentCommentTreeResponse.CommentNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        nodes.sort(NODE_ORDER);
        for (DocumentCommentTreeResponse.CommentNode n : nodes) {
            sortRecursive(n.getChildren());
        }
    }
}
