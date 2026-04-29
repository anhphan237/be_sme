package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentBlockUpdateRequest;
import com.sme.be_sme.modules.content.api.response.DocumentBlockMutateResponse;
import com.sme.be_sme.modules.content.config.DocumentBlockFeatureFlags;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.service.DocumentBlockSyncService;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentBlockMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentBlockEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class DocumentBlockUpdateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentBlockMapper documentBlockMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;
    private final DocumentBlockSyncService documentBlockSyncService;
    private final DocumentBlockFeatureFlags documentBlockFeatureFlags;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentBlockUpdateRequest request = objectMapper.convertValue(payload, DocumentBlockUpdateRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId()) || !StringUtils.hasText(request.getBlockId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId and blockId are required");
        }
        if (!documentBlockFeatureFlags.isWriteEnabled()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "document block write path is disabled");
        }
        String companyId = context.getTenantId().trim();
        String documentId = request.getDocumentId().trim();
        String blockId = request.getBlockId().trim();
        String operatorId = context.getOperatorId();
        Date now = new Date();

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);
        documentBlockSyncService.ensureBlocksBackfilled(companyId, documentId, doc.getDraftJson(), operatorId);

        DocumentBlockEntity row = documentBlockMapper.selectByPrimaryKey(blockId);
        if (row == null || !companyId.equals(row.getCompanyId()) || !documentId.equals(row.getDocumentId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "block not found");
        }
        if (request.getBlockType() != null && StringUtils.hasText(request.getBlockType())) {
            row.setBlockType(request.getBlockType().trim());
        }
        if (request.getProps() != null) {
            row.setPropsJson(writeJson(request.getProps()));
        }
        if (request.getContent() != null) {
            row.setContentJson(writeJson(request.getContent()));
        }
        row.setUpdatedAt(now);
        documentBlockMapper.updateByPrimaryKey(row);

        DocumentBlockMutateResponse response = new DocumentBlockMutateResponse();
        response.setDocumentId(documentId);
        response.setBlockId(blockId);
        response.setUpdatedAt(now);
        return response;
    }

    private String writeJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }
}
