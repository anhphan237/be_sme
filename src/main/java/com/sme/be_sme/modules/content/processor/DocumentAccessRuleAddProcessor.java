package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentAccessRuleAddRequest;
import com.sme.be_sme.modules.content.api.response.DocumentAccessRuleAddResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAccessRuleMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentActivityLogMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAccessRuleEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentActivityLogEntity;
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
public class DocumentAccessRuleAddProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentAccessRuleMapper documentAccessRuleMapper;
    private final DocumentActivityLogMapper documentActivityLogMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentAccessRuleAddRequest request = objectMapper.convertValue(payload, DocumentAccessRuleAddRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId is required");
        }
        String roleId = StringUtils.hasText(request.getRoleId()) ? request.getRoleId().trim() : null;
        String departmentId = StringUtils.hasText(request.getDepartmentId()) ? request.getDepartmentId().trim() : null;
        if (!StringUtils.hasText(roleId) && !StringUtils.hasText(departmentId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "roleId and/or departmentId is required");
        }

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        String documentId = request.getDocumentId().trim();

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        Date now = new Date();
        String ruleId = UuidGenerator.generate();
        DocumentAccessRuleEntity row = new DocumentAccessRuleEntity();
        row.setDocumentAccessRuleId(ruleId);
        row.setCompanyId(companyId);
        row.setDocumentId(documentId);
        row.setRoleId(roleId);
        row.setDepartmentId(departmentId);
        row.setStatus(DocumentEditorConstants.STATUS_ACTIVE);
        row.setCreatedAt(now);

        if (documentAccessRuleMapper.insert(row) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to add access rule");
        }

        DocumentActivityLogEntity log = new DocumentActivityLogEntity();
        log.setDocumentActivityLogId(UuidGenerator.generate());
        log.setCompanyId(companyId);
        log.setDocumentId(documentId);
        log.setAction(DocumentEditorConstants.ACTION_ACCESS_RULE_ADD);
        log.setActorUserId(operatorId);
        log.setDetailJson("{\"documentAccessRuleId\":\"" + ruleId + "\"}");
        log.setCreatedAt(now);
        documentActivityLogMapper.insert(log);

        DocumentAccessRuleAddResponse response = new DocumentAccessRuleAddResponse();
        response.setDocumentAccessRuleId(ruleId);
        response.setDocumentId(documentId);
        response.setCreatedAt(now);
        return response;
    }
}
