package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentAttachmentListRequest;
import com.sme.be_sme.modules.content.api.response.DocumentAttachmentListResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAttachmentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAttachmentEntity;
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
public class DocumentAttachmentListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentAttachmentMapper documentAttachmentMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentAttachmentListRequest request = objectMapper.convertValue(payload, DocumentAttachmentListRequest.class);
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

        List<DocumentAttachmentEntity> rows = documentAttachmentMapper.selectActiveByCompanyAndDocument(companyId, documentId, limit);
        List<DocumentAttachmentListResponse.AttachmentRow> items = new ArrayList<>();
        if (rows != null) {
            for (DocumentAttachmentEntity r : rows) {
                DocumentAttachmentListResponse.AttachmentRow row = new DocumentAttachmentListResponse.AttachmentRow();
                row.setDocumentAttachmentId(r.getDocumentAttachmentId());
                row.setFileUrl(r.getFileUrl());
                row.setFileName(r.getFileName());
                row.setFileType(r.getFileType());
                row.setFileSizeBytes(r.getFileSizeBytes());
                row.setMediaKind(r.getMediaKind());
                row.setUploadedAt(r.getUploadedAt());
                row.setUploadedBy(r.getUploadedBy());
                items.add(row);
            }
        }

        DocumentAttachmentListResponse response = new DocumentAttachmentListResponse();
        response.setDocumentId(documentId);
        response.setItems(items);
        return response;
    }
}
