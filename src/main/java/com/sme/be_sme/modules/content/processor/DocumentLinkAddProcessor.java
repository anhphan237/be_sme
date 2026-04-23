package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentLinkAddRequest;
import com.sme.be_sme.modules.content.api.response.DocumentLinkAddResponse;
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
import java.util.List;

@Component
@RequiredArgsConstructor
public class DocumentLinkAddProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentLinkMapper documentLinkMapper;
    private final DocumentActivityLogMapper documentActivityLogMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentLinkAddRequest request = objectMapper.convertValue(payload, DocumentLinkAddRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getSourceDocumentId())
                || !StringUtils.hasText(request.getTargetDocumentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "sourceDocumentId and targetDocumentId are required");
        }

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        String sourceId = request.getSourceDocumentId().trim();
        String targetId = request.getTargetDocumentId().trim();
        if (sourceId.equals(targetId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "cannot link document to itself");
        }

        String linkType = StringUtils.hasText(request.getLinkType())
                ? request.getLinkType().trim()
                : DocumentEditorConstants.DEFAULT_LINK_TYPE;

        DocumentEntity src = documentMapper.selectByPrimaryKey(sourceId);
        DocumentEntity tgt = documentMapper.selectByPrimaryKey(targetId);
        if (src == null || !companyId.equals(src.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "source document not found");
        }
        if (tgt == null || !companyId.equals(tgt.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "target document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, src);
        documentAccessEvaluator.assertCanAccess(context, tgt);

        List<DocumentLinkEntity> existing = documentLinkMapper.selectActiveOutgoingByCompanyAndSource(companyId, sourceId, 500);
        if (existing != null) {
            for (DocumentLinkEntity e : existing) {
                if (targetId.equals(e.getTargetDocumentId()) && linkType.equalsIgnoreCase(e.getLinkType())) {
                    throw AppException.of(ErrorCodes.BAD_REQUEST, "link already exists");
                }
            }
        }

        Date now = new Date();
        String linkId = UuidGenerator.generate();
        DocumentLinkEntity row = new DocumentLinkEntity();
        row.setDocumentLinkId(linkId);
        row.setCompanyId(companyId);
        row.setSourceDocumentId(sourceId);
        row.setTargetDocumentId(targetId);
        row.setLinkType(linkType);
        row.setStatus(DocumentEditorConstants.STATUS_ACTIVE);
        row.setCreatedBy(operatorId);
        row.setCreatedAt(now);
        if (documentLinkMapper.insert(row) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to add link");
        }

        DocumentActivityLogEntity log = new DocumentActivityLogEntity();
        log.setDocumentActivityLogId(UuidGenerator.generate());
        log.setCompanyId(companyId);
        log.setDocumentId(sourceId);
        log.setAction(DocumentEditorConstants.ACTION_LINK_ADD);
        log.setActorUserId(operatorId);
        log.setDetailJson("{\"documentLinkId\":\"" + linkId + "\",\"targetDocumentId\":\"" + targetId + "\"}");
        log.setCreatedAt(now);
        documentActivityLogMapper.insert(log);

        DocumentLinkAddResponse response = new DocumentLinkAddResponse();
        response.setDocumentLinkId(linkId);
        response.setSourceDocumentId(sourceId);
        response.setTargetDocumentId(targetId);
        response.setLinkType(linkType);
        return response;
    }
}
