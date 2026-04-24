package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentFolderDeleteRequest;
import com.sme.be_sme.modules.content.api.response.DocumentFolderDeleteResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentFolderItemMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentFolderMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentFolderEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class DocumentFolderDeleteProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentFolderMapper documentFolderMapper;
    private final DocumentFolderItemMapper documentFolderItemMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentFolderDeleteRequest request = objectMapper.convertValue(payload, DocumentFolderDeleteRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getFolderId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "folderId is required");
        }

        String companyId = context.getTenantId();
        String folderId = request.getFolderId().trim();

        DocumentFolderEntity folder = documentFolderMapper.selectByPrimaryKey(folderId);
        if (folder == null || !companyId.equals(folder.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "folder not found");
        }
        if (!DocumentEditorConstants.STATUS_ACTIVE.equalsIgnoreCase(
                folder.getStatus() != null ? folder.getStatus().trim() : "")) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "folder is not active");
        }

        int docsInside = documentFolderItemMapper.countByCompanyIdAndFolderId(companyId, folderId);
        if (docsInside > 0) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "folder contains documents");
        }

        int childCount = documentFolderMapper.countActiveChildrenByCompanyAndParentId(companyId, folderId);
        if (childCount > 0) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "folder has child folders");
        }

        folder.setStatus(DocumentEditorConstants.STATUS_DELETED);
        folder.setUpdatedAt(new Date());
        if (documentFolderMapper.updateByPrimaryKey(folder) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to delete folder");
        }

        DocumentFolderDeleteResponse response = new DocumentFolderDeleteResponse();
        response.setFolderId(folderId);
        response.setDeleted(true);
        return response;
    }
}
