package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentBlockCreateRequest;
import com.sme.be_sme.modules.content.api.response.DocumentBlockMutateResponse;
import com.sme.be_sme.modules.content.config.DocumentBlockFeatureFlags;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.service.BlockOrderService;
import com.sme.be_sme.modules.content.service.DocumentBlockSyncService;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentBlockMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentBlockEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class DocumentBlockCreateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentBlockMapper documentBlockMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;
    private final DocumentBlockSyncService documentBlockSyncService;
    private final BlockOrderService blockOrderService;
    private final DocumentBlockFeatureFlags documentBlockFeatureFlags;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentBlockCreateRequest request = objectMapper.convertValue(payload, DocumentBlockCreateRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId()) || !StringUtils.hasText(request.getBlockType())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId and blockType are required");
        }
        if (!documentBlockFeatureFlags.isWriteEnabled()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "document block write path is disabled");
        }
        String companyId = context.getTenantId().trim();
        String operatorId = context.getOperatorId();
        String documentId = request.getDocumentId().trim();
        Date now = new Date();

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null || !companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);
        documentBlockSyncService.ensureBlocksBackfilled(companyId, documentId, doc.getDraftJson(), operatorId);

        List<DocumentBlockEntity> allRows = documentBlockMapper.selectActiveByCompanyAndDocumentId(companyId, documentId);
        String parentBlockId = StringUtils.hasText(request.getParentBlockId()) ? request.getParentBlockId().trim() : null;
        List<DocumentBlockEntity> siblings = filterSiblings(allRows, parentBlockId);

        DocumentBlockEntity newRow = new DocumentBlockEntity();
        newRow.setDocumentBlockId(UuidGenerator.generate());
        newRow.setCompanyId(companyId);
        newRow.setDocumentId(documentId);
        newRow.setParentBlockId(parentBlockId);
        newRow.setBlockType(request.getBlockType().trim());
        newRow.setPropsJson(writeJson(request.getProps()));
        newRow.setContentJson(writeJson(request.getContent()));
        newRow.setOrderKey(BlockOrderService.formatOrder(siblings.size() + 1));
        newRow.setStatus("ACTIVE");
        newRow.setCreatedBy(operatorId);
        newRow.setCreatedAt(now);
        newRow.setUpdatedAt(now);
        documentBlockMapper.insert(newRow);

        List<DocumentBlockEntity> reordered = new ArrayList<>(siblings);
        reordered.add(newRow);
        reordered = blockOrderService.reorderSiblings(reordered, newRow.getDocumentBlockId(),
                StringUtils.hasText(request.getAfterBlockId()) ? request.getAfterBlockId().trim() : null);
        for (DocumentBlockEntity row : reordered) {
            row.setUpdatedAt(now);
            documentBlockMapper.updateByPrimaryKey(row);
        }

        DocumentBlockMutateResponse response = new DocumentBlockMutateResponse();
        response.setDocumentId(documentId);
        response.setBlockId(newRow.getDocumentBlockId());
        response.setUpdatedAt(now);
        return response;
    }

    private static List<DocumentBlockEntity> filterSiblings(List<DocumentBlockEntity> rows, String parentBlockId) {
        List<DocumentBlockEntity> result = new ArrayList<>();
        if (rows == null) {
            return result;
        }
        for (DocumentBlockEntity row : rows) {
            if (Objects.equals(parentBlockId, row.getParentBlockId())) {
                result.add(row);
            }
        }
        return result;
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
