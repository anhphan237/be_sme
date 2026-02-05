package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentUploadRequest;
import com.sme.be_sme.modules.content.api.response.DocumentUploadResponse;
import com.sme.be_sme.modules.content.support.DocumentChunker;
import com.sme.be_sme.modules.content.support.DocumentDownloader;
import com.sme.be_sme.modules.content.support.DocumentTextExtractor;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentChunkMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentVersionMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentChunkEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentVersionEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DocumentUploadProcessor extends BaseBizProcessor<BizContext> {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadProcessor.class);
    private static final int VERSION_NO = 1;

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper documentVersionMapper;
    private final DocumentChunkMapper documentChunkMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentUploadRequest request = objectMapper.convertValue(payload, DocumentUploadRequest.class);
        validate(context, request);

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        Date now = new Date();

        String documentId = UuidGenerator.generate();
        DocumentEntity doc = new DocumentEntity();
        doc.setDocumentId(documentId);
        doc.setCompanyId(companyId);
        doc.setTitle(request.getName().trim());
        doc.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        doc.setDocumentCategoryId(StringUtils.hasText(request.getDocumentCategoryId()) ? request.getDocumentCategoryId().trim() : null);
        doc.setVisibility("TENANT");
        doc.setStatus("ACTIVE");
        doc.setCreatedBy(operatorId);
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);

        if (documentMapper.insert(doc) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to create document");
        }

        String fileUrl = StringUtils.hasText(request.getFileUrl()) ? request.getFileUrl().trim() : null;
        if (fileUrl != null) {
            DocumentVersionEntity version = new DocumentVersionEntity();
            version.setDocumentVersionId(UuidGenerator.generate());
            version.setCompanyId(companyId);
            version.setDocumentId(documentId);
            version.setVersionNo(VERSION_NO);
            version.setFileUrl(fileUrl);
            version.setFileName(request.getName() != null ? request.getName().trim() : null);
            version.setUploadedBy(operatorId);
            version.setUploadedAt(now);
            documentVersionMapper.insert(version);

            storeChunksForDocument(companyId, documentId, fileUrl, request.getName());
        }

        DocumentUploadResponse response = new DocumentUploadResponse();
        response.setDocumentId(documentId);
        response.setName(doc.getTitle());
        response.setFileUrl(fileUrl);
        response.setDescription(doc.getDescription());
        return response;
    }

    private void storeChunksForDocument(String companyId, String documentId, String fileUrl, String documentTitle) {
        byte[] bytes = DocumentDownloader.download(fileUrl);
        if (bytes == null) {
            log.warn("Could not download document from {}, skipping chunking for documentId={}", fileUrl, documentId);
            return;
        }
        String ext = getFileExtension(fileUrl, documentTitle);
        String rawText = DocumentTextExtractor.extractText(bytes, ext);
        String normalizedText = DocumentChunker.normalize(rawText);
        if (!StringUtils.hasText(normalizedText) || normalizedText.length() < 50) {
            log.debug("No or too short extracted text for documentId={}, skipping chunks", documentId);
            return;
        }
        List<String> chunks = DocumentChunker.chunk(normalizedText);
        if (chunks.isEmpty()) return;

        documentChunkMapper.deleteByDocumentIdAndVersion(companyId, documentId, VERSION_NO);

        Date now = new Date();
        List<DocumentChunkEntity> entities = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunkEntity e = new DocumentChunkEntity();
            e.setChunkId(UuidGenerator.generate());
            e.setCompanyId(companyId);
            e.setDocumentId(documentId);
            e.setVersionNo(VERSION_NO);
            e.setChunkNo(i + 1);
            e.setChunkText(chunks.get(i));
            e.setCreatedAt(now);
            entities.add(e);
        }
        documentChunkMapper.insertBatch(entities);
    }

    private static String getFileExtension(String fileUrl, String fileName) {
        if (StringUtils.hasText(fileUrl)) {
            int q = fileUrl.indexOf('?');
            String path = q >= 0 ? fileUrl.substring(0, q) : fileUrl;
            int last = path.lastIndexOf('.');
            if (last >= 0 && last < path.length() - 1) {
                return path.substring(last + 1).trim();
            }
        }
        if (StringUtils.hasText(fileName)) {
            int last = fileName.lastIndexOf('.');
            if (last >= 0 && last < fileName.length() - 1) {
                return fileName.substring(last + 1).trim();
            }
        }
        return "";
    }

    private static void validate(BizContext context, DocumentUploadRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is required");
        }
    }
}
