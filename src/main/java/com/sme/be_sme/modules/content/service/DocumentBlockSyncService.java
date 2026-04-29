package com.sme.be_sme.modules.content.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.config.DocumentBlockFeatureFlags;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentBlockMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentBlockEntity;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentBlockSyncService {

    private final ObjectMapper objectMapper;
    private final DocumentBlockMapper documentBlockMapper;
    private final DocumentBlockFeatureFlags featureFlags;

    public void syncFromDraftJson(String companyId, String documentId, String draftJson, String operatorId, Date now) {
        if (!StringUtils.hasText(companyId) || !StringUtils.hasText(documentId)) {
            return;
        }
        if (!featureFlags.isWriteEnabled()) {
            return;
        }
        JsonNode root = parseJson(draftJson);
        documentBlockMapper.deleteByCompanyAndDocumentId(companyId, documentId);
        if (root == null) {
            return;
        }

        JsonNode content = root.get("content");
        if (content == null || !content.isArray()) {
            return;
        }
        int order = 1;
        for (JsonNode blockNode : content) {
            saveBlockRecursive(companyId, documentId, null, blockNode, operatorId, now, order++);
        }
    }

    public void ensureBlocksBackfilled(String companyId, String documentId, String draftJson, String operatorId) {
        List<DocumentBlockEntity> existing = documentBlockMapper.selectActiveByCompanyAndDocumentId(companyId, documentId);
        if (existing != null && !existing.isEmpty()) {
            return;
        }
        syncFromDraftJson(companyId, documentId, draftJson, operatorId, new Date());
    }

    public List<DocumentBlockEntity> listBlocks(String companyId, String documentId) {
        if (!featureFlags.isReadEnabled()) {
            return new ArrayList<>();
        }
        return documentBlockMapper.selectActiveByCompanyAndDocumentId(companyId, documentId);
    }

    private void saveBlockRecursive(
            String companyId,
            String documentId,
            String parentBlockId,
            JsonNode blockNode,
            String operatorId,
            Date now,
            int siblingOrder
    ) {
        if (blockNode == null || !blockNode.isObject()) {
            return;
        }
        DocumentBlockEntity row = new DocumentBlockEntity();
        String blockId = UuidGenerator.generate();
        row.setDocumentBlockId(blockId);
        row.setCompanyId(companyId);
        row.setDocumentId(documentId);
        row.setParentBlockId(parentBlockId);
        row.setBlockType(StringUtils.hasText(text(blockNode.get("type"))) ? text(blockNode.get("type")) : "paragraph");
        row.setPropsJson(toJson(blockNode.get("attrs")));
        row.setContentJson(toJson(blockNode));
        row.setOrderKey(BlockOrderService.formatOrder(siblingOrder));
        row.setStatus("ACTIVE");
        row.setCreatedBy(operatorId);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        documentBlockMapper.insert(row);

        JsonNode children = blockNode.get("content");
        if (children != null && children.isArray()) {
            int childOrder = 1;
            Iterator<JsonNode> it = children.elements();
            while (it.hasNext()) {
                saveBlockRecursive(companyId, documentId, blockId, it.next(), operatorId, now, childOrder++);
            }
        }
    }

    private JsonNode parseJson(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String toJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    public JsonNode buildDraftJsonFromBlocks(List<DocumentBlockEntity> blocks) {
        List<JsonNode> roots = new ArrayList<>();
        if (blocks == null || blocks.isEmpty()) {
            return objectMapper.createObjectNode().put("type", "doc").set("content", objectMapper.createArrayNode());
        }
        for (DocumentBlockEntity row : blocks) {
            if (!StringUtils.hasText(row.getParentBlockId())) {
                roots.add(parseJson(row.getContentJson()));
            }
        }
        return objectMapper.createObjectNode().put("type", "doc").set("content", objectMapper.valueToTree(roots));
    }
}
