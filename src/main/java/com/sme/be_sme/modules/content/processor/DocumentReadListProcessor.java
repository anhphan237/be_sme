package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentReadListRequest;
import com.sme.be_sme.modules.content.api.response.DocumentReadListResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAcknowledgementMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAcknowledgementEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
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
public class DocumentReadListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentAcknowledgementMapper documentAcknowledgementMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;
    private final UserMapperExt userMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentReadListRequest request = objectMapper.convertValue(payload, DocumentReadListRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId is required");
        }

        String companyId = context.getTenantId();
        String documentId = request.getDocumentId().trim();
        int limit = request.getLimit() != null && request.getLimit() > 0
                ? Math.min(request.getLimit(), 500) : 100;

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        if (!companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "document does not belong to tenant");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);

        List<DocumentAcknowledgementEntity> rows = documentAcknowledgementMapper
                .selectByCompanyAndDocumentOrderByLastInteraction(companyId, documentId, limit);
        List<DocumentReadListResponse.ReadRow> items = new ArrayList<>();
        if (rows != null) {
            for (DocumentAcknowledgementEntity r : rows) {
                DocumentReadListResponse.ReadRow item = new DocumentReadListResponse.ReadRow();
                item.setUserId(r.getUserId());
                UserEntity user = userMapperExt.selectByCompanyIdAndUserId(companyId, r.getUserId());
                if (user != null) {
                    item.setFullName(user.getFullName());
                    item.setEmail(user.getEmail());
                }
                item.setStatus(r.getStatus());
                item.setReadAt(r.getReadAt());
                item.setAckedAt(r.getAckedAt());
                items.add(item);
            }
        }

        DocumentReadListResponse response = new DocumentReadListResponse();
        response.setDocumentId(documentId);
        response.setItems(items);
        return response;
    }
}
