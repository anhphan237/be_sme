package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentListRequest;
import com.sme.be_sme.modules.content.api.response.DocumentListResponse;
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
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DocumentListProcessor extends BaseBizProcessor<BizContext> {

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
        List<DocumentEntity> docs = documentMapper.selectByCompanyId(companyId);
        if (docs == null) {
            docs = new ArrayList<>();
        }
        if (StringUtils.hasText(request.getDocumentCategoryId())) {
            String catId = request.getDocumentCategoryId().trim();
            docs = docs.stream().filter(d -> catId.equals(d.getDocumentCategoryId())).collect(Collectors.toList());
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
        return response;
    }

    private String getLatestFileUrl(String documentId) {
        List<DocumentVersionEntity> versions = documentVersionMapper.selectByDocumentId(documentId);
        if (versions == null || versions.isEmpty()) return null;
        return versions.get(0).getFileUrl();
    }
}
