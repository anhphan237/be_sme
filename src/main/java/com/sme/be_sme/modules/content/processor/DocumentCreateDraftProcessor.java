package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentCreateDraftRequest;
import com.sme.be_sme.modules.content.api.response.DocumentCreateDraftResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
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
public class DocumentCreateDraftProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentActivityLogMapper documentActivityLogMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentCreateDraftRequest request = objectMapper.convertValue(payload, DocumentCreateDraftRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "request is required");
        }

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        Date now = new Date();

        String title = StringUtils.hasText(request.getTitle()) ? request.getTitle().trim() : "Untitled";
        String draftRaw = request.getDraftJson();
        String draftNormalized = normalizeDraftJson(draftRaw);

        String documentId = UuidGenerator.generate();
        DocumentEntity doc = new DocumentEntity();
        doc.setDocumentId(documentId);
        doc.setCompanyId(companyId);
        doc.setTitle(title);
        doc.setDescription(null);
        doc.setDocumentCategoryId(StringUtils.hasText(request.getDocumentCategoryId())
                ? request.getDocumentCategoryId().trim() : null);
        doc.setVisibility("TENANT");
        doc.setStatus(DocumentEditorConstants.STATUS_DRAFT);
        doc.setContentKind(DocumentEditorConstants.CONTENT_KIND_EDITOR);
        doc.setDraftJson(draftNormalized);
        doc.setPublishedJson(null);
        doc.setPublishedAt(null);
        doc.setPublishedBy(null);
        doc.setCreatedBy(operatorId);
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);

        if (documentMapper.insert(doc) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to create document");
        }

        insertActivity(companyId, documentId, operatorId, DocumentEditorConstants.ACTION_DRAFT_CREATE, null, now);

        DocumentCreateDraftResponse response = new DocumentCreateDraftResponse();
        response.setDocumentId(documentId);
        response.setTitle(title);
        response.setStatus(doc.getStatus());
        return response;
    }

    private String normalizeDraftJson(String draftRaw) {
        if (!StringUtils.hasText(draftRaw)) {
            return DocumentEditorConstants.DEFAULT_EMPTY_JSON;
        }
        String trimmed = draftRaw.trim();
        try {
            objectMapper.readTree(trimmed);
            return trimmed;
        } catch (Exception ex) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "draftJson must be valid JSON");
        }
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
