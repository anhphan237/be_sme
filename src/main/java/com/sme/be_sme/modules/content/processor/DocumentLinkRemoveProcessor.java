package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentLinkRemoveRequest;
import com.sme.be_sme.modules.content.api.response.DocumentLinkRemoveResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentActivityLogMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentLinkMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentActivityLogEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentLinkEntity;
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
public class DocumentLinkRemoveProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentLinkMapper documentLinkMapper;
    private final DocumentActivityLogMapper documentActivityLogMapper;
    private final DocumentMapper documentMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentLinkRemoveRequest request = objectMapper.convertValue(payload, DocumentLinkRemoveRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentLinkId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentLinkId is required");
        }

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        String linkId = request.getDocumentLinkId().trim();

        DocumentLinkEntity row = documentLinkMapper.selectByPrimaryKey(linkId);
        if (row == null || !companyId.equals(row.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "link not found");
        }

        DocumentEntity src = documentMapper.selectByPrimaryKey(row.getSourceDocumentId());
        DocumentEntity tgt = documentMapper.selectByPrimaryKey(row.getTargetDocumentId());
        if (src == null || !companyId.equals(src.getCompanyId()) || tgt == null || !companyId.equals(tgt.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, src);
        documentAccessEvaluator.assertCanAccess(context, tgt);

        int deleted = documentLinkMapper.deleteByPrimaryKey(linkId);
        Date now = new Date();
        DocumentActivityLogEntity log = new DocumentActivityLogEntity();
        log.setDocumentActivityLogId(UuidGenerator.generate());
        log.setCompanyId(companyId);
        log.setDocumentId(row.getSourceDocumentId());
        log.setAction(DocumentEditorConstants.ACTION_LINK_REMOVE);
        log.setActorUserId(operatorId);
        log.setDetailJson("{\"documentLinkId\":\"" + linkId + "\"}");
        log.setCreatedAt(now);
        documentActivityLogMapper.insert(log);

        DocumentLinkRemoveResponse response = new DocumentLinkRemoveResponse();
        response.setRemoved(deleted > 0);
        return response;
    }
}
