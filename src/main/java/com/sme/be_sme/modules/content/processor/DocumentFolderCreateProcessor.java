package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentFolderCreateRequest;
import com.sme.be_sme.modules.content.api.response.DocumentFolderCreateResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentFolderMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentFolderEntity;
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
public class DocumentFolderCreateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentFolderMapper documentFolderMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentFolderCreateRequest request = objectMapper.convertValue(payload, DocumentFolderCreateRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is required");
        }

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        Date now = new Date();

        String parentId = StringUtils.hasText(request.getParentFolderId()) ? request.getParentFolderId().trim() : null;
        if (parentId != null) {
            DocumentFolderEntity parent = documentFolderMapper.selectByPrimaryKey(parentId);
            if (parent == null || !companyId.equals(parent.getCompanyId())) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "parent folder not found");
            }
        }

        String folderId = UuidGenerator.generate();
        DocumentFolderEntity row = new DocumentFolderEntity();
        row.setFolderId(folderId);
        row.setCompanyId(companyId);
        row.setParentFolderId(parentId);
        row.setName(request.getName().trim());
        row.setSortOrder(0);
        row.setStatus(DocumentEditorConstants.STATUS_ACTIVE);
        row.setCreatedBy(operatorId);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);

        if (documentFolderMapper.insert(row) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to create folder");
        }

        DocumentFolderCreateResponse response = new DocumentFolderCreateResponse();
        response.setFolderId(folderId);
        response.setName(row.getName());
        response.setParentFolderId(parentId);
        return response;
    }
}
