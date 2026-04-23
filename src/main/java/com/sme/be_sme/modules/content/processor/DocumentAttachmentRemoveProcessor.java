package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentAttachmentRemoveRequest;
import com.sme.be_sme.modules.content.api.response.DocumentAttachmentRemoveResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentActivityLogMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAttachmentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentActivityLogEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAttachmentEntity;
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
public class DocumentAttachmentRemoveProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentAttachmentMapper documentAttachmentMapper;
    private final DocumentActivityLogMapper documentActivityLogMapper;
    private final DocumentMapper documentMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentAttachmentRemoveRequest request = objectMapper.convertValue(payload, DocumentAttachmentRemoveRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentAttachmentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentAttachmentId is required");
        }

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        String attachmentId = request.getDocumentAttachmentId().trim();

        DocumentAttachmentEntity row = documentAttachmentMapper.selectByPrimaryKey(attachmentId);
        if (row == null || !companyId.equals(row.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "attachment not found");
        }

        DocumentEntity doc = documentMapper.selectByPrimaryKey(row.getDocumentId());
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        int deleted = documentAttachmentMapper.deleteByPrimaryKey(attachmentId);
        Date now = new Date();
        DocumentActivityLogEntity log = new DocumentActivityLogEntity();
        log.setDocumentActivityLogId(UuidGenerator.generate());
        log.setCompanyId(companyId);
        log.setDocumentId(row.getDocumentId());
        log.setAction(DocumentEditorConstants.ACTION_ATTACHMENT_REMOVE);
        log.setActorUserId(operatorId);
        log.setDetailJson("{\"documentAttachmentId\":\"" + attachmentId + "\"}");
        log.setCreatedAt(now);
        documentActivityLogMapper.insert(log);

        DocumentAttachmentRemoveResponse response = new DocumentAttachmentRemoveResponse();
        response.setRemoved(deleted > 0);
        return response;
    }
}
