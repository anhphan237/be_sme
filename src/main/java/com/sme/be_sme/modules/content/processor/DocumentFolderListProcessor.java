package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentFolderListRequest;
import com.sme.be_sme.modules.content.api.response.DocumentFolderListResponse;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentFolderMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentFolderEntity;
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
public class DocumentFolderListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentFolderMapper documentFolderMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        if (payload != null && !payload.isNull()) {
            objectMapper.convertValue(payload, DocumentFolderListRequest.class);
        }
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        String companyId = context.getTenantId();
        List<DocumentFolderEntity> rows = documentFolderMapper.selectByCompanyId(companyId);
        List<DocumentFolderListResponse.FolderItem> items = new ArrayList<>();
        if (rows != null) {
            for (DocumentFolderEntity f : rows) {
                DocumentFolderListResponse.FolderItem item = new DocumentFolderListResponse.FolderItem();
                item.setFolderId(f.getFolderId());
                item.setParentFolderId(f.getParentFolderId());
                item.setName(f.getName());
                item.setSortOrder(f.getSortOrder());
                item.setCreatedAt(f.getCreatedAt());
                items.add(item);
            }
        }

        DocumentFolderListResponse response = new DocumentFolderListResponse();
        response.setItems(items);
        return response;
    }
}
