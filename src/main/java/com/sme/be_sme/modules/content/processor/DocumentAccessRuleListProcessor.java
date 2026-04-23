package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentAccessRuleListRequest;
import com.sme.be_sme.modules.content.api.response.DocumentAccessRuleListResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAccessRuleMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAccessRuleEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DocumentAccessRuleListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentAccessRuleMapper documentAccessRuleMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentAccessRuleListRequest request = objectMapper.convertValue(payload, DocumentAccessRuleListRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId is required");
        }

        String companyId = context.getTenantId();
        String documentId = request.getDocumentId().trim();

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        List<DocumentAccessRuleEntity> rows =
                documentAccessRuleMapper.selectActiveByCompanyAndDocumentId(companyId, documentId);
        List<DocumentAccessRuleListResponse.Row> out = new ArrayList<>();
        if (rows != null) {
            for (DocumentAccessRuleEntity r : rows) {
                DocumentAccessRuleListResponse.Row row = new DocumentAccessRuleListResponse.Row();
                row.setDocumentAccessRuleId(r.getDocumentAccessRuleId());
                row.setRoleId(r.getRoleId());
                row.setDepartmentId(r.getDepartmentId());
                row.setStatus(r.getStatus());
                row.setCreatedAt(r.getCreatedAt());
                out.add(row);
            }
        }

        DocumentAccessRuleListResponse response = new DocumentAccessRuleListResponse();
        response.setDocumentId(documentId);
        response.setRules(out);
        return response;
    }
}
