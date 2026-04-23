package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentAssignmentListRequest;
import com.sme.be_sme.modules.content.api.response.DocumentAssignmentListResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAssignmentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAssignmentEntity;
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
public class DocumentAssignmentListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentAssignmentMapper documentAssignmentMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentAssignmentListRequest request = objectMapper.convertValue(payload, DocumentAssignmentListRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId is required");
        }

        String companyId = context.getTenantId();
        String documentId = request.getDocumentId().trim();
        int limit = request.getLimit() != null && request.getLimit() > 0 ? Math.min(request.getLimit(), 500) : 100;

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        List<DocumentAssignmentEntity> rows = documentAssignmentMapper.selectActiveByCompanyAndDocument(companyId, documentId, limit);
        List<DocumentAssignmentListResponse.AssignmentRow> items = new ArrayList<>();
        if (rows != null) {
            for (DocumentAssignmentEntity r : rows) {
                DocumentAssignmentListResponse.AssignmentRow row = new DocumentAssignmentListResponse.AssignmentRow();
                row.setDocumentAssignmentId(r.getDocumentAssignmentId());
                row.setAssigneeUserId(r.getAssigneeUserId());
                row.setAssignedByUserId(r.getAssignedByUserId());
                row.setStatus(r.getStatus());
                row.setAssignedAt(r.getAssignedAt());
                items.add(row);
            }
        }

        DocumentAssignmentListResponse response = new DocumentAssignmentListResponse();
        response.setDocumentId(documentId);
        response.setItems(items);
        return response;
    }
}
