package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentFolderTreeRequest;
import com.sme.be_sme.modules.content.api.response.DocumentFolderTreeResponse;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentFolderMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentFolderEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DocumentFolderTreeProcessor extends BaseBizProcessor<BizContext> {

    private static final Comparator<DocumentFolderTreeResponse.FolderTreeNode> NODE_ORDER =
            Comparator.comparing(DocumentFolderTreeResponse.FolderTreeNode::getSortOrder,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(n -> n.getName() != null ? n.getName() : "",
                            String.CASE_INSENSITIVE_ORDER);

    private final ObjectMapper objectMapper;
    private final DocumentFolderMapper documentFolderMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        if (payload != null && !payload.isNull()) {
            objectMapper.convertValue(payload, DocumentFolderTreeRequest.class);
        }
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        String companyId = context.getTenantId();
        List<DocumentFolderEntity> rows = documentFolderMapper.selectByCompanyId(companyId);
        if (rows == null) {
            rows = new ArrayList<>();
        }

        Map<String, DocumentFolderTreeResponse.FolderTreeNode> byId = new HashMap<>();
        for (DocumentFolderEntity f : rows) {
            DocumentFolderTreeResponse.FolderTreeNode n = new DocumentFolderTreeResponse.FolderTreeNode();
            n.setFolderId(f.getFolderId());
            n.setParentFolderId(f.getParentFolderId());
            n.setName(f.getName());
            n.setSortOrder(f.getSortOrder());
            n.setCreatedAt(f.getCreatedAt());
            byId.put(f.getFolderId(), n);
        }

        List<DocumentFolderTreeResponse.FolderTreeNode> roots = new ArrayList<>();
        for (DocumentFolderEntity f : rows) {
            DocumentFolderTreeResponse.FolderTreeNode node = byId.get(f.getFolderId());
            String pid = f.getParentFolderId();
            if (!StringUtils.hasText(pid) || !byId.containsKey(pid.trim())) {
                roots.add(node);
            } else {
                byId.get(pid.trim()).getChildren().add(node);
            }
        }

        sortRecursive(roots);

        DocumentFolderTreeResponse response = new DocumentFolderTreeResponse();
        response.setRoots(roots);
        return response;
    }

    private static void sortRecursive(List<DocumentFolderTreeResponse.FolderTreeNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        nodes.sort(NODE_ORDER);
        for (DocumentFolderTreeResponse.FolderTreeNode n : nodes) {
            sortRecursive(n.getChildren());
        }
    }
}
