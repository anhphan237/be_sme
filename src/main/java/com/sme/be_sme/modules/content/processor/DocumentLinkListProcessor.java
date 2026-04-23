package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentLinkListRequest;
import com.sme.be_sme.modules.content.api.response.DocumentLinkListResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentLinkMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentLinkEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class DocumentLinkListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentLinkMapper documentLinkMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentLinkListRequest request = objectMapper.convertValue(payload, DocumentLinkListRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId is required");
        }

        String companyId = context.getTenantId();
        String documentId = request.getDocumentId().trim();
        String dir = StringUtils.hasText(request.getDirection()) ? request.getDirection().trim().toUpperCase(Locale.ROOT) : "OUT";
        int limit = request.getLimit() != null && request.getLimit() > 0 ? Math.min(request.getLimit(), 200) : 100;

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        List<DocumentLinkListResponse.LinkRow> items = new ArrayList<>();
        if ("IN".equals(dir)) {
            appendIncoming(companyId, documentId, limit, items);
        } else if ("BOTH".equals(dir)) {
            appendOutgoing(companyId, documentId, limit, items);
            appendIncoming(companyId, documentId, limit, items);
        } else {
            appendOutgoing(companyId, documentId, limit, items);
        }

        DocumentLinkListResponse response = new DocumentLinkListResponse();
        response.setDocumentId(documentId);
        response.setItems(items);
        return response;
    }

    private void appendOutgoing(String companyId, String documentId, int limit, List<DocumentLinkListResponse.LinkRow> items) {
        List<DocumentLinkEntity> rows = documentLinkMapper.selectActiveOutgoingByCompanyAndSource(companyId, documentId, limit);
        if (rows == null) {
            return;
        }
        for (DocumentLinkEntity e : rows) {
            DocumentLinkListResponse.LinkRow row = new DocumentLinkListResponse.LinkRow();
            row.setDocumentLinkId(e.getDocumentLinkId());
            row.setLinkedDocumentId(e.getTargetDocumentId());
            row.setLinkType(e.getLinkType());
            row.setDirection("OUT");
            row.setCreatedAt(e.getCreatedAt());
            row.setCreatedBy(e.getCreatedBy());
            items.add(row);
        }
    }

    private void appendIncoming(String companyId, String documentId, int limit, List<DocumentLinkListResponse.LinkRow> items) {
        List<DocumentLinkEntity> rows = documentLinkMapper.selectActiveIncomingByCompanyAndTarget(companyId, documentId, limit);
        if (rows == null) {
            return;
        }
        for (DocumentLinkEntity e : rows) {
            DocumentLinkListResponse.LinkRow row = new DocumentLinkListResponse.LinkRow();
            row.setDocumentLinkId(e.getDocumentLinkId());
            row.setLinkedDocumentId(e.getSourceDocumentId());
            row.setLinkType(e.getLinkType());
            row.setDirection("IN");
            row.setCreatedAt(e.getCreatedAt());
            row.setCreatedBy(e.getCreatedBy());
            items.add(row);
        }
    }
}
