package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentAccessRuleRemoveRequest;
import com.sme.be_sme.modules.content.api.response.DocumentAccessRuleRemoveResponse;
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
public class DocumentAccessRuleRemoveProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentAccessRuleMapper documentAccessRuleMapper;
    private final DocumentActivityLogMapper documentActivityLogMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentAccessRuleRemoveRequest request = objectMapper.convertValue(payload, DocumentAccessRuleRemoveRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentAccessRuleId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentAccessRuleId is required");
        }

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();
        String ruleId = request.getDocumentAccessRuleId().trim();

        DocumentAccessRuleEntity rule = documentAccessRuleMapper.selectByPrimaryKey(ruleId);
        if (rule == null || !companyId.equals(rule.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "access rule not found");
        }

        DocumentEntity doc = documentMapper.selectByPrimaryKey(rule.getDocumentId());
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        int deleted = documentAccessRuleMapper.deleteByPrimaryKey(ruleId);
        Date now = new Date();
        DocumentActivityLogEntity log = new DocumentActivityLogEntity();
        log.setDocumentActivityLogId(UuidGenerator.generate());
        log.setCompanyId(companyId);
        log.setDocumentId(rule.getDocumentId());
        log.setAction(DocumentEditorConstants.ACTION_ACCESS_RULE_REMOVE);
        log.setActorUserId(operatorId);
        log.setDetailJson("{\"documentAccessRuleId\":\"" + ruleId + "\"}");
        log.setCreatedAt(now);
        documentActivityLogMapper.insert(log);

        DocumentAccessRuleRemoveResponse response = new DocumentAccessRuleRemoveResponse();
        response.setRemoved(deleted > 0);
        return response;
    }
}
