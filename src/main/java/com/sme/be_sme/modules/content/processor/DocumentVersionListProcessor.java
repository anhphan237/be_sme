package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentVersionListRequest;
import com.sme.be_sme.modules.content.api.response.DocumentVersionListResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentVersionMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentVersionEntity;
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
public class DocumentVersionListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper documentVersionMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentVersionListRequest request = objectMapper.convertValue(payload, DocumentVersionListRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId is required");
        }

        String companyId = context.getTenantId();
        String documentId = request.getDocumentId().trim();

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        if (!companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "document does not belong to tenant");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        List<DocumentVersionEntity> versions = documentVersionMapper.selectByDocumentId(documentId);
        if (versions == null) {
            versions = new ArrayList<>();
        }

        List<DocumentVersionListResponse.VersionItem> items = new ArrayList<>();
        for (DocumentVersionEntity v : versions) {
            DocumentVersionListResponse.VersionItem item = new DocumentVersionListResponse.VersionItem();
            item.setDocumentVersionId(v.getDocumentVersionId());
            item.setVersionNo(v.getVersionNo());
            item.setFileUrl(v.getFileUrl());
            item.setRichTextSnapshot(StringUtils.hasText(v.getContentJson()));
            item.setUploadedAt(v.getUploadedAt());
            item.setUploadedBy(v.getUploadedBy());
            items.add(item);
        }

        DocumentVersionListResponse response = new DocumentVersionListResponse();
        response.setDocumentId(documentId);
        response.setItems(items);
        return response;
    }
}
