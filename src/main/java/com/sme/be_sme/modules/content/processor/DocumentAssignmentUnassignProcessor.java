package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentAssignmentUnassignRequest;
import com.sme.be_sme.modules.content.api.response.DocumentAssignmentUnassignResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentActivityLogMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAssignmentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentActivityLogEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAssignmentEntity;
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
public class DocumentAssignmentUnassignProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentAssignmentMapper documentAssignmentMapper;
    private final DocumentActivityLogMapper documentActivityLogMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentAssignmentUnassignRequest request = objectMapper.convertValue(payload, DocumentAssignmentUnassignRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId()) || !StringUtils.hasText(request.getAssigneeUserId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId and assigneeUserId are required");
        }

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        String documentId = request.getDocumentId().trim();
        String assigneeId = request.getAssigneeUserId().trim();

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        DocumentAssignmentEntity active = documentAssignmentMapper.selectActiveByCompanyDocumentAndAssignee(
                companyId, documentId, assigneeId);
        DocumentAssignmentUnassignResponse response = new DocumentAssignmentUnassignResponse();
        if (active == null) {
            response.setRevoked(false);
            return response;
        }

        Date now = new Date();
        active.setStatus(DocumentEditorConstants.ASSIGNMENT_STATUS_REVOKED);
        active.setUpdatedAt(now);
        if (documentAssignmentMapper.updateByPrimaryKey(active) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to unassign");
        }

        DocumentActivityLogEntity log = new DocumentActivityLogEntity();
        log.setDocumentActivityLogId(UuidGenerator.generate());
        log.setCompanyId(companyId);
        log.setDocumentId(documentId);
        log.setAction(DocumentEditorConstants.ACTION_ASSIGNMENT_REVOKED);
        log.setActorUserId(operatorId);
        log.setDetailJson("{\"documentAssignmentId\":\"" + active.getDocumentAssignmentId()
                + "\",\"assigneeUserId\":\"" + assigneeId + "\"}");
        log.setCreatedAt(now);
        documentActivityLogMapper.insert(log);

        response.setRevoked(true);
        return response;
    }
}
