package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentFolderAddDocumentRequest;
import com.sme.be_sme.modules.content.api.response.DocumentFolderAddDocumentResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentFolderMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentFolderItemMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentFolderEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentFolderItemEntity;
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
public class DocumentFolderAddDocumentProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentFolderMapper documentFolderMapper;
    private final DocumentFolderItemMapper documentFolderItemMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentFolderAddDocumentRequest request = objectMapper.convertValue(payload, DocumentFolderAddDocumentRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getFolderId()) || !StringUtils.hasText(request.getDocumentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "folderId and documentId are required");
        }

        String companyId = context.getTenantId();
        String folderId = request.getFolderId().trim();
        String documentId = request.getDocumentId().trim();

        DocumentFolderEntity folder = documentFolderMapper.selectByPrimaryKey(folderId);
        if (folder == null || !companyId.equals(folder.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "folder not found");
        }

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        if (!DocumentEditorConstants.CONTENT_KIND_EDITOR.equalsIgnoreCase(doc.getContentKind())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "only EDITOR documents can be placed in folders");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        Date now = new Date();
        documentFolderItemMapper.deleteByCompanyIdAndDocumentId(companyId, documentId);

        DocumentFolderItemEntity item = new DocumentFolderItemEntity();
        String itemId = UuidGenerator.generate();
        item.setDocumentFolderItemId(itemId);
        item.setCompanyId(companyId);
        item.setFolderId(folderId);
        item.setDocumentId(documentId);
        item.setCreatedAt(now);
        if (documentFolderItemMapper.insert(item) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to assign document to folder");
        }

        DocumentFolderAddDocumentResponse response = new DocumentFolderAddDocumentResponse();
        response.setDocumentFolderItemId(itemId);
        response.setFolderId(folderId);
        response.setDocumentId(documentId);
        return response;
    }
}
