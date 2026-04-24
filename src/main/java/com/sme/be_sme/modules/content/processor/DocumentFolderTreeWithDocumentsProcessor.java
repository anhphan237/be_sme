package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentFolderTreeWithDocumentsRequest;
import com.sme.be_sme.modules.content.api.response.DocumentFolderTreeWithDocumentsResponse;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.doceditor.DocumentEditorConstants;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentFolderItemMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentFolderMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentFolderEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentFolderItemEntity;
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
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DocumentFolderTreeWithDocumentsProcessor extends BaseBizProcessor<BizContext> {

    private static final Comparator<DocumentFolderTreeWithDocumentsResponse.FolderNode> FOLDER_ORDER =
            Comparator.comparing(DocumentFolderTreeWithDocumentsResponse.FolderNode::getSortOrder,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(n -> n.getName() != null ? n.getName() : "",
                            String.CASE_INSENSITIVE_ORDER);

    private static final Comparator<DocumentFolderTreeWithDocumentsResponse.DocumentNode> DOC_ORDER =
            Comparator.comparing(DocumentFolderTreeWithDocumentsResponse.DocumentNode::getUpdatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(n -> n.getTitle() != null ? n.getTitle() : "",
                            String.CASE_INSENSITIVE_ORDER);

    private final ObjectMapper objectMapper;
    private final DocumentFolderMapper documentFolderMapper;
    private final DocumentFolderItemMapper documentFolderItemMapper;
    private final DocumentMapper documentMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentFolderTreeWithDocumentsRequest request = payload == null || payload.isNull()
                ? new DocumentFolderTreeWithDocumentsRequest()
                : objectMapper.convertValue(payload, DocumentFolderTreeWithDocumentsRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        String companyId = context.getTenantId();
        String titleQuery = StringUtils.hasText(request.getTitleQuery())
                ? request.getTitleQuery().trim().toLowerCase(Locale.ROOT)
                : null;

        List<DocumentFolderEntity> folderRows = documentFolderMapper.selectByCompanyId(companyId);
        if (folderRows == null) {
            folderRows = new ArrayList<>();
        }
        Map<String, DocumentFolderTreeWithDocumentsResponse.FolderNode> byFolderId = new HashMap<>();
        for (DocumentFolderEntity f : folderRows) {
            DocumentFolderTreeWithDocumentsResponse.FolderNode node = new DocumentFolderTreeWithDocumentsResponse.FolderNode();
            node.setFolderId(f.getFolderId());
            node.setParentFolderId(f.getParentFolderId());
            node.setName(f.getName());
            node.setSortOrder(f.getSortOrder());
            node.setCreatedAt(f.getCreatedAt());
            byFolderId.put(f.getFolderId(), node);
        }

        List<DocumentFolderTreeWithDocumentsResponse.FolderNode> roots = new ArrayList<>();
        for (DocumentFolderEntity f : folderRows) {
            DocumentFolderTreeWithDocumentsResponse.FolderNode node = byFolderId.get(f.getFolderId());
            String pid = f.getParentFolderId();
            if (!StringUtils.hasText(pid) || !byFolderId.containsKey(pid.trim())) {
                roots.add(node);
            } else {
                byFolderId.get(pid.trim()).getChildren().add(node);
            }
        }

        List<DocumentEntity> docs = documentMapper.selectByCompanyIdAndContentKind(
                companyId, DocumentEditorConstants.CONTENT_KIND_EDITOR);
        if (docs == null) {
            docs = new ArrayList<>();
        }
        Map<String, DocumentEntity> docById = new HashMap<>();
        for (DocumentEntity d : docs) {
            if (titleQuery != null) {
                String title = d.getTitle() != null ? d.getTitle().toLowerCase(Locale.ROOT) : "";
                if (!title.contains(titleQuery)) {
                    continue;
                }
            }
            try {
                documentAccessEvaluator.assertCanAccess(context, d);
                docById.put(d.getDocumentId(), d);
            } catch (AppException ex) {
                if (!ErrorCodes.FORBIDDEN.equals(ex.getCode())) {
                    throw ex;
                }
            }
        }

        List<DocumentFolderItemEntity> folderItems = documentFolderItemMapper.selectByCompanyId(companyId);
        if (folderItems != null) {
            for (DocumentFolderItemEntity item : folderItems) {
                DocumentFolderTreeWithDocumentsResponse.FolderNode folder = byFolderId.get(item.getFolderId());
                DocumentEntity d = docById.get(item.getDocumentId());
                if (folder == null || d == null) {
                    continue;
                }
                DocumentFolderTreeWithDocumentsResponse.DocumentNode dn =
                        new DocumentFolderTreeWithDocumentsResponse.DocumentNode();
                dn.setDocumentId(d.getDocumentId());
                dn.setTitle(d.getTitle());
                dn.setStatus(d.getStatus());
                dn.setUpdatedAt(d.getUpdatedAt());
                dn.setPublished(d.getPublishedAt() != null);
                folder.getDocuments().add(dn);
            }
        }

        sortRecursive(roots);

        DocumentFolderTreeWithDocumentsResponse response = new DocumentFolderTreeWithDocumentsResponse();
        response.setRoots(roots);
        return response;
    }

    private static void sortRecursive(List<DocumentFolderTreeWithDocumentsResponse.FolderNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        nodes.sort(FOLDER_ORDER);
        for (DocumentFolderTreeWithDocumentsResponse.FolderNode n : nodes) {
            n.getDocuments().sort(DOC_ORDER);
            sortRecursive(n.getChildren());
        }
    }
}
