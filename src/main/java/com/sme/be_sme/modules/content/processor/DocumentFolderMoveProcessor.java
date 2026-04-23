package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentFolderMoveRequest;
import com.sme.be_sme.modules.content.api.response.DocumentFolderMoveResponse;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentFolderMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentFolderEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DocumentFolderMoveProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentFolderMapper documentFolderMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentFolderMoveRequest request = objectMapper.convertValue(payload, DocumentFolderMoveRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getFolderId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "folderId is required");
        }

        String companyId = context.getTenantId();
        String folderId = request.getFolderId().trim();
        String newParentRaw = request.getNewParentFolderId();
        String newParentId = StringUtils.hasText(newParentRaw) ? newParentRaw.trim() : null;

        DocumentFolderEntity row = documentFolderMapper.selectByPrimaryKey(folderId);
        if (row == null || !companyId.equals(row.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "folder not found");
        }

        if (newParentId != null) {
            if (newParentId.equals(folderId)) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "cannot move folder under itself");
            }
            DocumentFolderEntity parent = documentFolderMapper.selectByPrimaryKey(newParentId);
            if (parent == null || !companyId.equals(parent.getCompanyId())) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "new parent folder not found");
            }
            List<DocumentFolderEntity> all = documentFolderMapper.selectByCompanyId(companyId);
            Set<String> descendants = collectDescendantFolderIds(folderId, all);
            if (descendants.contains(newParentId)) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "cannot move folder under its descendant");
            }
        }

        row.setParentFolderId(newParentId);
        row.setUpdatedAt(new Date());
        if (documentFolderMapper.updateByPrimaryKey(row) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to move folder");
        }

        DocumentFolderMoveResponse response = new DocumentFolderMoveResponse();
        response.setFolderId(folderId);
        response.setParentFolderId(newParentId);
        return response;
    }

    private static Set<String> collectDescendantFolderIds(String rootFolderId, List<DocumentFolderEntity> all) {
        Set<String> out = new HashSet<>();
        if (all == null || all.isEmpty()) {
            return out;
        }
        Queue<String> q = new ArrayDeque<>();
        for (DocumentFolderEntity f : all) {
            if (rootFolderId.equals(f.getParentFolderId())) {
                q.add(f.getFolderId());
            }
        }
        while (!q.isEmpty()) {
            String id = q.poll();
            if (!out.add(id)) {
                continue;
            }
            for (DocumentFolderEntity f : all) {
                if (id.equals(f.getParentFolderId())) {
                    q.add(f.getFolderId());
                }
            }
        }
        return out;
    }
}
