package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentSoftDeleteRequest;
import com.sme.be_sme.modules.content.api.response.DocumentSoftDeleteResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentChunkMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentFolderItemMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class DocumentSoftDeleteProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;
    private final DocumentFolderItemMapper documentFolderItemMapper;
    private final DocumentChunkMapper documentChunkMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentSoftDeleteRequest request = objectMapper.convertValue(payload, DocumentSoftDeleteRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId is required");
        }

        String companyId = context.getTenantId();
        String documentId = request.getDocumentId().trim();

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }

        DocumentSoftDeleteResponse response = new DocumentSoftDeleteResponse();
        response.setDocumentId(documentId);

        if (DocumentEditorConstants.STATUS_DELETED.equalsIgnoreCase(
                doc.getStatus() != null ? doc.getStatus().trim() : "")) {
            documentAccessEvaluator.assertCanMutateIncludingSoftDeleted(context, doc);
            response.setDeleted(false);
            return response;
        }

        documentAccessEvaluator.assertCanAccess(context, doc);

        doc.setStatus(DocumentEditorConstants.STATUS_DELETED);
        doc.setUpdatedAt(new Date());

        documentFolderItemMapper.deleteByCompanyIdAndDocumentId(companyId, documentId);
        documentChunkMapper.deleteByDocumentId(companyId, documentId);

        if (documentMapper.updateByPrimaryKey(doc) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to delete document");
        }

        response.setDeleted(true);
        return response;
    }
}
