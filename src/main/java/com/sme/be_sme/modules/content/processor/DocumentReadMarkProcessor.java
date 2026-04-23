package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentReadMarkRequest;
import com.sme.be_sme.modules.content.api.response.DocumentReadMarkResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAcknowledgementMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAcknowledgementEntity;
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
public class DocumentReadMarkProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentAcknowledgementMapper documentAcknowledgementMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentReadMarkRequest request = objectMapper.convertValue(payload, DocumentReadMarkRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId is required");
        }

        String companyId = context.getTenantId();
        String userId = context.getOperatorId();
        String documentId = request.getDocumentId().trim();
        String onboardingScope = DocumentEditorConstants.GENERAL_READ_ONBOARDING_ID;

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        if (!companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "document does not belong to tenant");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        Date now = new Date();
        DocumentAcknowledgementEntity existing = documentAcknowledgementMapper
                .selectByCompanyIdAndDocumentIdAndUserIdAndOnboardingId(companyId, documentId, userId, onboardingScope);

        String ackId;
        if (existing != null) {
            ackId = existing.getDocumentAcknowledgementId();
            existing.setReadAt(now);
            String st = existing.getStatus() != null ? existing.getStatus().trim() : "";
            if (!"ACKED".equalsIgnoreCase(st)) {
                existing.setStatus(DocumentEditorConstants.STATUS_READ);
            }
            documentAcknowledgementMapper.updateByPrimaryKey(existing);
        } else {
            ackId = UuidGenerator.generate();
            DocumentAcknowledgementEntity row = new DocumentAcknowledgementEntity();
            row.setDocumentAcknowledgementId(ackId);
            row.setCompanyId(companyId);
            row.setDocumentId(documentId);
            row.setUserId(userId);
            row.setOnboardingId(onboardingScope);
            row.setStatus(DocumentEditorConstants.STATUS_READ);
            row.setReadAt(now);
            row.setAckedAt(null);
            row.setCreatedAt(now);
            documentAcknowledgementMapper.insert(row);
        }

        DocumentReadMarkResponse response = new DocumentReadMarkResponse();
        response.setDocumentAcknowledgementId(ackId);
        response.setDocumentId(documentId);
        response.setReadAt(now);
        return response;
    }
}
