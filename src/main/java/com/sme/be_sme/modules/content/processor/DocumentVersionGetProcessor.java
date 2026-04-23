package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentVersionGetRequest;
import com.sme.be_sme.modules.content.api.response.DocumentVersionGetResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentVersionMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentVersionEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class DocumentVersionGetProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentVersionMapper documentVersionMapper;
    private final DocumentMapper documentMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentVersionGetRequest request = objectMapper.convertValue(payload, DocumentVersionGetRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentVersionId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentVersionId is required");
        }

        String companyId = context.getTenantId();
        String versionId = request.getDocumentVersionId().trim();

        DocumentVersionEntity v = documentVersionMapper.selectByPrimaryKey(versionId);
        if (v == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "version not found");
        }
        if (!companyId.equals(v.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "version does not belong to tenant");
        }

        DocumentEntity doc = documentMapper.selectByPrimaryKey(v.getDocumentId());
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        DocumentVersionGetResponse response = new DocumentVersionGetResponse();
        response.setDocumentVersionId(v.getDocumentVersionId());
        response.setDocumentId(v.getDocumentId());
        response.setVersionNo(v.getVersionNo());
        response.setFileUrl(v.getFileUrl());
        response.setUploadedAt(v.getUploadedAt());
        response.setUploadedBy(v.getUploadedBy());
        response.setContentJson(parseJson(v.getContentJson()));
        return response;
    }

    private JsonNode parseJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ex) {
            return null;
        }
    }
}
