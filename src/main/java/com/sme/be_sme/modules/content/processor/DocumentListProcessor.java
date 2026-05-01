package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentListRequest;
import com.sme.be_sme.modules.content.api.response.DocumentListResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
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
public class DocumentListProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper documentVersionMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentListRequest request = payload != null && !payload.isNull()
                ? objectMapper.convertValue(payload, DocumentListRequest.class)
                : new DocumentListRequest();
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        String companyId = context.getTenantId();
        String contentKind = DocumentEditorConstants.CONTENT_KIND_FILE;

        int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 1;
        int pageSize = request.getPageSize() != null && request.getPageSize() > 0
                ? Math.min(request.getPageSize(), MAX_PAGE_SIZE)
                : DEFAULT_PAGE_SIZE;
        int offset = (page - 1) * pageSize;

        String categoryFilter = StringUtils.hasText(request.getDocumentCategoryId())
                ? request.getDocumentCategoryId().trim()
                : null;

        String createdByFilter = null;
        if (Boolean.TRUE.equals(request.getOnlyMine())) {
            String operatorId = context.getOperatorId();
            if (!StringUtils.hasText(operatorId)) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "operatorId is required for onlyMine");
            }
            createdByFilter = operatorId.trim();
        }

        long total = documentMapper.countContentFileDocumentsPaged(
                companyId, contentKind, categoryFilter, createdByFilter);

        List<DocumentEntity> docs = documentMapper.selectContentFileDocumentsPaged(
                companyId, contentKind, categoryFilter, createdByFilter, pageSize, offset);
        if (docs == null) {
            docs = new ArrayList<>();
        }

        List<DocumentListResponse.DocumentItem> items = new ArrayList<>();
        for (DocumentEntity d : docs) {
            String fileUrl = getLatestFileUrl(d.getDocumentId());
            DocumentListResponse.DocumentItem item = new DocumentListResponse.DocumentItem();
            item.setDocumentId(d.getDocumentId());
            item.setName(d.getTitle());
            item.setFileUrl(fileUrl);
            item.setDescription(d.getDescription());
            item.setStatus(d.getStatus());
            items.add(item);
        }

        DocumentListResponse response = new DocumentListResponse();
        response.setItems(items);
        response.setTotalCount(total);
        response.setPage(page);
        response.setPageSize(pageSize);
        return response;
    }

    private String getLatestFileUrl(String documentId) {
        List<DocumentVersionEntity> versions = documentVersionMapper.selectByDocumentId(documentId);
        if (versions == null || versions.isEmpty()) return null;
        return versions.get(0).getFileUrl();
    }
}
