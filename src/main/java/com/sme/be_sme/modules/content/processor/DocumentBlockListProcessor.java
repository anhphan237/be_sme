package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentBlockListRequest;
import com.sme.be_sme.modules.content.api.response.DocumentBlockListResponse;
import com.sme.be_sme.modules.content.config.DocumentBlockFeatureFlags;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.service.DocumentBlockSyncService;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentBlockEntity;
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
public class DocumentBlockListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;
    private final DocumentBlockSyncService documentBlockSyncService;
    private final DocumentBlockFeatureFlags documentBlockFeatureFlags;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentBlockListRequest request = objectMapper.convertValue(payload, DocumentBlockListRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId is required");
        }
        if (!documentBlockFeatureFlags.isReadEnabled()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "document block read path is disabled");
        }
        String companyId = context.getTenantId().trim();
        String operatorId = context.getOperatorId();
        String documentId = request.getDocumentId().trim();

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);
        documentBlockSyncService.ensureBlocksBackfilled(companyId, documentId, doc.getDraftJson(), operatorId);
        List<DocumentBlockEntity> rows = documentBlockSyncService.listBlocks(companyId, documentId);

        List<DocumentBlockListResponse.BlockItem> items = new ArrayList<>();
        if (rows != null) {
            for (DocumentBlockEntity row : rows) {
                DocumentBlockListResponse.BlockItem item = new DocumentBlockListResponse.BlockItem();
                item.setBlockId(row.getDocumentBlockId());
                item.setParentBlockId(row.getParentBlockId());
                item.setBlockType(row.getBlockType());
                item.setProps(parseJson(row.getPropsJson()));
                item.setContent(parseJson(row.getContentJson()));
                item.setOrderKey(row.getOrderKey());
                item.setCreatedAt(row.getCreatedAt());
                item.setUpdatedAt(row.getUpdatedAt());
                items.add(item);
            }
        }
        DocumentBlockListResponse response = new DocumentBlockListResponse();
        response.setDocumentId(documentId);
        response.setItems(items);
        return response;
    }

    private JsonNode parseJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return null;
        }
    }
}
