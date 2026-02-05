package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentUploadRequest;
import com.sme.be_sme.modules.content.api.response.DocumentUploadResponse;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentVersionMapper;
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

import java.util.Date;

@Component
@RequiredArgsConstructor
public class DocumentUploadProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper documentVersionMapper;

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
            version.setVersionNo(1);
            version.setFileUrl(fileUrl);
            version.setFileName(request.getName() != null ? request.getName().trim() : null);
            version.setUploadedBy(operatorId);
            version.setUploadedAt(now);
            documentVersionMapper.insert(version);
        }

        DocumentUploadResponse response = new DocumentUploadResponse();
        response.setDocumentId(documentId);
        response.setName(doc.getTitle());
        response.setFileUrl(fileUrl);
        response.setDescription(doc.getDescription());
        return response;
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
