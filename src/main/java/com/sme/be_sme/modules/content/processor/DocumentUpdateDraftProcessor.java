package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentUpdateDraftRequest;
import com.sme.be_sme.modules.content.api.response.DocumentEditorSaveResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
import com.sme.be_sme.modules.content.service.DocumentBlockSyncService;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentActivityLogMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentActivityLogEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
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
public class DocumentUpdateDraftProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentActivityLogMapper documentActivityLogMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;
    private final DocumentBlockSyncService documentBlockSyncService;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentUpdateDraftRequest request = objectMapper.convertValue(payload, DocumentUpdateDraftRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId is required");
        }
        if (!StringUtils.hasText(request.getTitle()) && !StringUtils.hasText(request.getDraftJson())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "title or draftJson is required");
        }

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        String documentId = request.getDocumentId().trim();

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        if (!companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "document does not belong to tenant");
        }
        if (!DocumentEditorConstants.CONTENT_KIND_EDITOR.equalsIgnoreCase(doc.getContentKind())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "document is not an editor document");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        Date now = new Date();
        if (StringUtils.hasText(request.getTitle())) {
            doc.setTitle(request.getTitle().trim());
        }
        if (StringUtils.hasText(request.getDraftJson())) {
            String trimmed = request.getDraftJson().trim();
            try {
                objectMapper.readTree(trimmed);
            } catch (Exception ex) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "draftJson must be valid JSON");
            }
            doc.setDraftJson(trimmed);
        }
        doc.setUpdatedAt(now);

        if (documentMapper.updateByPrimaryKey(doc) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to update document");
        }
        documentBlockSyncService.syncFromDraftJson(companyId, documentId, doc.getDraftJson(), operatorId, now);

        insertActivity(companyId, documentId, operatorId, DocumentEditorConstants.ACTION_DRAFT_SAVE, null, now);

        DocumentEditorSaveResponse response = new DocumentEditorSaveResponse();
        response.setDocumentId(documentId);
        response.setUpdatedAt(now);
        return response;
    }

    private void insertActivity(String companyId, String documentId, String actorUserId, String action,
                                String detailJson, Date now) {
        DocumentActivityLogEntity row = new DocumentActivityLogEntity();
        row.setDocumentActivityLogId(UuidGenerator.generate());
        row.setCompanyId(companyId);
        row.setDocumentId(documentId);
        row.setAction(action);
        row.setActorUserId(actorUserId);
        row.setDetailJson(detailJson);
        row.setCreatedAt(now);
        documentActivityLogMapper.insert(row);
    }
}
