package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentPublishRequest;
import com.sme.be_sme.modules.content.api.response.DocumentPublishResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
import com.sme.be_sme.modules.content.support.DocumentChunker;
import com.sme.be_sme.modules.content.support.DocumentEditorTextExtractor;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentActivityLogMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentChunkMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentVersionMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentActivityLogEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentChunkEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentVersionEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DocumentPublishProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper documentVersionMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final DocumentActivityLogMapper documentActivityLogMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentPublishRequest request = objectMapper.convertValue(payload, DocumentPublishRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId is required");
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

        String snapshot = StringUtils.hasText(doc.getDraftJson()) ? doc.getDraftJson().trim() : DocumentEditorConstants.DEFAULT_EMPTY_JSON;
        try {
            objectMapper.readTree(snapshot);
        } catch (Exception ex) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "draftJson must be valid JSON before publish");
        }

        Date now = new Date();
        Integer maxNo = documentVersionMapper.selectMaxVersionNoByDocumentId(documentId);
        int nextVersion = (maxNo == null ? 0 : maxNo) + 1;

        String versionId = UuidGenerator.generate();
        DocumentVersionEntity version = new DocumentVersionEntity();
        version.setDocumentVersionId(versionId);
        version.setCompanyId(companyId);
        version.setDocumentId(documentId);
        version.setVersionNo(nextVersion);
        version.setFileUrl(null);
        version.setFileName((doc.getTitle() != null ? doc.getTitle().trim() : "document") + ".json");
        version.setFileType("application/json");
        byte[] bytes = snapshot.getBytes(StandardCharsets.UTF_8);
        version.setFileSizeBytes((long) bytes.length);
        version.setContentJson(snapshot);
        version.setUploadedBy(operatorId);
        version.setUploadedAt(now);
        if (documentVersionMapper.insert(version) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to insert document version");
        }

        doc.setPublishedJson(snapshot);
        doc.setPublishedAt(now);
        doc.setPublishedBy(operatorId);
        doc.setStatus(DocumentEditorConstants.STATUS_ACTIVE);
        doc.setUpdatedAt(now);
        if (documentMapper.updateByPrimaryKey(doc) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to update document");
        }
        upsertChunksForPublishedEditorDocument(companyId, documentId, snapshot, nextVersion, now);

        DocumentActivityLogEntity logRow = new DocumentActivityLogEntity();
        logRow.setDocumentActivityLogId(UuidGenerator.generate());
        logRow.setCompanyId(companyId);
        logRow.setDocumentId(documentId);
        logRow.setAction(DocumentEditorConstants.ACTION_PUBLISH);
        logRow.setActorUserId(operatorId);
        logRow.setDetailJson("{\"versionNo\":" + nextVersion + "}");
        logRow.setCreatedAt(now);
        documentActivityLogMapper.insert(logRow);

        DocumentPublishResponse response = new DocumentPublishResponse();
        response.setDocumentId(documentId);
        response.setVersionNo(nextVersion);
        response.setDocumentVersionId(versionId);
        return response;
    }

    private void upsertChunksForPublishedEditorDocument(
            String companyId,
            String documentId,
            String publishedJson,
            int versionNo,
            Date now) {
        String extracted = DocumentEditorTextExtractor.extractText(objectMapper, publishedJson);
        String normalized = DocumentChunker.normalize(extracted);

        // Keep only latest searchable snapshot for an editor document.
        documentChunkMapper.deleteByDocumentId(companyId, documentId);
        if (!StringUtils.hasText(normalized) || normalized.length() < 50) {
            return;
        }

        List<String> chunks = DocumentChunker.chunk(normalized);
        if (chunks.isEmpty()) {
            return;
        }

        List<DocumentChunkEntity> rows = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunkEntity row = new DocumentChunkEntity();
            row.setChunkId(UuidGenerator.generate());
            row.setCompanyId(companyId);
            row.setDocumentId(documentId);
            row.setVersionNo(versionNo);
            row.setChunkNo(i + 1);
            row.setChunkText(chunks.get(i));
            row.setCreatedAt(now);
            rows.add(row);
        }
        documentChunkMapper.insertBatch(rows);
    }
}
