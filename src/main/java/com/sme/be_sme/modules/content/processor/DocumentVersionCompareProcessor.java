package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sme.be_sme.modules.content.api.request.DocumentVersionCompareRequest;
import com.sme.be_sme.modules.content.api.response.DocumentVersionCompareResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.doceditor.DocumentVersionJsonDiff;
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
public class DocumentVersionCompareProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper documentVersionMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentVersionCompareRequest request = objectMapper.convertValue(payload, DocumentVersionCompareRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId())
                || !StringUtils.hasText(request.getFromDocumentVersionId())
                || !StringUtils.hasText(request.getToDocumentVersionId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId, fromDocumentVersionId, toDocumentVersionId are required");
        }

        String companyId = context.getTenantId();
        String documentId = request.getDocumentId().trim();
        String fromId = request.getFromDocumentVersionId().trim();
        String toId = request.getToDocumentVersionId().trim();

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        DocumentVersionEntity vFrom = documentVersionMapper.selectByPrimaryKey(fromId);
        DocumentVersionEntity vTo = documentVersionMapper.selectByPrimaryKey(toId);
        if (vFrom == null || !companyId.equals(vFrom.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "from version not found");
        }
        if (vTo == null || !companyId.equals(vTo.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "to version not found");
        }
        if (!documentId.equals(vFrom.getDocumentId()) || !documentId.equals(vTo.getDocumentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "versions do not belong to document");
        }

        boolean equal = DocumentVersionJsonDiff.equalDeep(objectMapper, vFrom.getContentJson(), vTo.getContentJson());
        ObjectNode summary = DocumentVersionJsonDiff.buildSummary(objectMapper, vFrom.getContentJson(), vTo.getContentJson());

        DocumentVersionCompareResponse response = new DocumentVersionCompareResponse();
        response.setDocumentId(documentId);
        response.setEqual(equal);
        response.setFromDocumentVersionId(fromId);
        response.setToDocumentVersionId(toId);
        response.setFromVersionNo(vFrom.getVersionNo());
        response.setToVersionNo(vTo.getVersionNo());
        response.setSummary(summary);
        response.setChanges(DocumentVersionJsonDiff.buildChanges(objectMapper, vFrom.getContentJson(), vTo.getContentJson()));
        response.setBlockChanges(DocumentVersionJsonDiff.buildBlockChanges(objectMapper, vFrom.getContentJson(), vTo.getContentJson()));
        return response;
    }
}
