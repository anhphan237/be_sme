package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentFolderRenameRequest;
import com.sme.be_sme.modules.content.api.response.DocumentFolderRenameResponse;
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
public class DocumentFolderRenameProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentFolderMapper documentFolderMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentFolderRenameRequest request = objectMapper.convertValue(payload, DocumentFolderRenameRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getFolderId()) || !StringUtils.hasText(request.getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "folderId and name are required");
        }

        String companyId = context.getTenantId();
        String folderId = request.getFolderId().trim();

        DocumentFolderEntity row = documentFolderMapper.selectByPrimaryKey(folderId);
        if (row == null || !companyId.equals(row.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "folder not found");
        }

        row.setName(request.getName().trim());
        row.setUpdatedAt(new Date());
        if (documentFolderMapper.updateByPrimaryKey(row) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to rename folder");
        }

        DocumentFolderRenameResponse response = new DocumentFolderRenameResponse();
        response.setFolderId(folderId);
        response.setName(row.getName());
        return response;
    }
}
