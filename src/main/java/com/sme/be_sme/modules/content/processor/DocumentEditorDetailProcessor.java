package com.sme.be_sme.modules.content.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.content.api.request.DocumentEditorDetailRequest;
import com.sme.be_sme.modules.content.api.response.DocumentEditorDetailResponse;
import com.sme.be_sme.modules.content.config.DocumentBlockFeatureFlags;
import com.sme.be_sme.modules.content.doceditor.DocumentAccessEvaluator;
import com.sme.be_sme.modules.content.doceditor.DocumentDetailInclude;
import com.sme.be_sme.modules.content.service.DocumentBlockSyncService;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAccessRuleMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentActivityLogMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAcknowledgementMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAssignmentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentAttachmentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentBlockMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentCommentMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentFolderItemMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentFolderMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentLinkMapper;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAccessRuleEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentActivityLogEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAcknowledgementEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAssignmentEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentAttachmentEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentBlockEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentCommentEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentFolderEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentFolderItemEntity;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentLinkEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DocumentEditorDetailProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final DocumentMapper documentMapper;
    private final DocumentActivityLogMapper documentActivityLogMapper;
    private final DocumentAcknowledgementMapper documentAcknowledgementMapper;
    private final DocumentFolderMapper documentFolderMapper;
    private final DocumentFolderItemMapper documentFolderItemMapper;
    private final DocumentCommentMapper documentCommentMapper;
    private final DocumentLinkMapper documentLinkMapper;
    private final DocumentAssignmentMapper documentAssignmentMapper;
    private final DocumentAttachmentMapper documentAttachmentMapper;
    private final DocumentBlockMapper documentBlockMapper;
    private final DocumentAccessRuleMapper documentAccessRuleMapper;
    private final DocumentAccessEvaluator documentAccessEvaluator;
    private final DocumentBlockSyncService documentBlockSyncService;
    private final DocumentBlockFeatureFlags documentBlockFeatureFlags;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        DocumentEditorDetailRequest request = objectMapper.convertValue(payload, DocumentEditorDetailRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getDocumentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "documentId is required");
        }

        String companyId = context.getTenantId();
        String documentId = request.getDocumentId().trim();
        int activityLimit = request.getActivityLimit() != null && request.getActivityLimit() > 0
                ? Math.min(request.getActivityLimit(), 200) : 50;
        int readLimit = request.getReadLimit() != null && request.getReadLimit() > 0
                ? Math.min(request.getReadLimit(), 500) : 100;
        int commentLimit = request.getCommentLimit() != null && request.getCommentLimit() > 0
                ? Math.min(request.getCommentLimit(), 500) : 200;
        int relationLimit = request.getRelationLimit() != null && request.getRelationLimit() > 0
                ? Math.min(request.getRelationLimit(), 200) : 100;
        DocumentDetailInclude inc = DocumentDetailInclude.parse(request.getInclude());

        DocumentEntity doc = documentMapper.selectByPrimaryKey(documentId);
        if (doc == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "document not found");
        }
        if (!companyId.equals(doc.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "document does not belong to tenant");
        }
        documentAccessEvaluator.assertCanAccess(context, doc);
        documentBlockSyncService.ensureBlocksBackfilled(companyId, documentId, doc.getDraftJson(), context.getOperatorId());

        DocumentEditorDetailResponse response = new DocumentEditorDetailResponse();
        response.setDocumentId(doc.getDocumentId());
        response.setTitle(doc.getTitle());
        response.setDescription(doc.getDescription());
        response.setStatus(doc.getStatus());
        response.setContentKind(doc.getContentKind());
        response.setDraftContent(parseJson(doc.getDraftJson()));
        response.setPublishedContent(parseJson(doc.getPublishedJson()));
        response.setPublishedAt(doc.getPublishedAt());
        response.setPublishedBy(doc.getPublishedBy());
        response.setCreatedAt(doc.getCreatedAt());
        response.setUpdatedAt(doc.getUpdatedAt());

        List<DocumentActivityLogEntity> logs = documentActivityLogMapper.selectRecentByDocumentId(
                companyId, documentId, activityLimit);
        List<DocumentEditorDetailResponse.ActivityItem> activityItems = new ArrayList<>();
        if (logs != null) {
            for (DocumentActivityLogEntity log : logs) {
                DocumentEditorDetailResponse.ActivityItem item = new DocumentEditorDetailResponse.ActivityItem();
                item.setAction(log.getAction());
                item.setActorUserId(log.getActorUserId());
                item.setDetail(parseJson(log.getDetailJson()));
                item.setCreatedAt(log.getCreatedAt());
                activityItems.add(item);
            }
        }
        response.setActivity(activityItems);

        List<DocumentAcknowledgementEntity> reads = documentAcknowledgementMapper
                .selectByCompanyAndDocumentOrderByLastInteraction(companyId, documentId, readLimit);
        List<DocumentEditorDetailResponse.ReadItem> readItems = new ArrayList<>();
        if (reads != null) {
            for (DocumentAcknowledgementEntity r : reads) {
                DocumentEditorDetailResponse.ReadItem item = new DocumentEditorDetailResponse.ReadItem();
                item.setUserId(r.getUserId());
                item.setStatus(r.getStatus());
                item.setReadAt(r.getReadAt());
                item.setAckedAt(r.getAckedAt());
                readItems.add(item);
            }
        }
        response.setReads(readItems);

        DocumentFolderItemEntity folderItem = documentFolderItemMapper.selectByCompanyIdAndDocumentId(companyId, documentId);
        if (folderItem == null) {
            response.setFolderPlacement(null);
        } else {
            List<DocumentFolderEntity> allFolders = documentFolderMapper.selectByCompanyId(companyId);
            Map<String, DocumentFolderEntity> byId = new HashMap<>();
            if (allFolders != null) {
                for (DocumentFolderEntity f : allFolders) {
                    byId.put(f.getFolderId(), f);
                }
            }
            List<String> pathNames = new ArrayList<>();
            String walk = folderItem.getFolderId();
            int guard = 0;
            while (walk != null && guard++ < 200) {
                DocumentFolderEntity f = byId.get(walk);
                if (f == null) {
                    break;
                }
                pathNames.add(0, f.getName());
                walk = f.getParentFolderId();
            }
            DocumentFolderEntity leaf = byId.get(folderItem.getFolderId());
            DocumentEditorDetailResponse.FolderPlacement fp = new DocumentEditorDetailResponse.FolderPlacement();
            fp.setFolderId(folderItem.getFolderId());
            fp.setFolderName(leaf != null ? leaf.getName() : null);
            fp.setPath(pathNames);
            response.setFolderPlacement(fp);
        }

        List<DocumentCommentEntity> commentRows = documentCommentMapper
                .selectByCompanyIdAndDocumentIdOrderByCreatedAsc(companyId, documentId, commentLimit);
        List<DocumentEditorDetailResponse.CommentItem> commentItems = new ArrayList<>();
        if (commentRows != null) {
            for (DocumentCommentEntity c : commentRows) {
                DocumentEditorDetailResponse.CommentItem item = new DocumentEditorDetailResponse.CommentItem();
                item.setCommentId(c.getDocumentCommentId());
                item.setParentCommentId(c.getParentCommentId());
                item.setAnchorBlockId(c.getAnchorBlockId());
                item.setAnchorStart(c.getAnchorStart());
                item.setAnchorEnd(c.getAnchorEnd());
                item.setAnchorText(c.getAnchorText());
                item.setAuthorUserId(c.getAuthorUserId());
                item.setBody(c.getBody());
                item.setStatus(c.getStatus());
                item.setCreatedAt(c.getCreatedAt());
                item.setUpdatedAt(c.getUpdatedAt());
                commentItems.add(item);
            }
        }
        response.setComments(commentItems);

        if (inc.links) {
            List<DocumentEditorDetailResponse.LinkItem> linkItems = new ArrayList<>();
            List<DocumentLinkEntity> out = documentLinkMapper.selectActiveOutgoingByCompanyAndSource(
                    companyId, documentId, relationLimit);
            if (out != null) {
                for (DocumentLinkEntity e : out) {
                    DocumentEditorDetailResponse.LinkItem item = new DocumentEditorDetailResponse.LinkItem();
                    item.setDocumentLinkId(e.getDocumentLinkId());
                    item.setLinkedDocumentId(e.getTargetDocumentId());
                    item.setLinkType(e.getLinkType());
                    item.setDirection("OUT");
                    item.setCreatedAt(e.getCreatedAt());
                    item.setCreatedBy(e.getCreatedBy());
                    linkItems.add(item);
                }
            }
            List<DocumentLinkEntity> inRows = documentLinkMapper.selectActiveIncomingByCompanyAndTarget(
                    companyId, documentId, relationLimit);
            if (inRows != null) {
                for (DocumentLinkEntity e : inRows) {
                    DocumentEditorDetailResponse.LinkItem item = new DocumentEditorDetailResponse.LinkItem();
                    item.setDocumentLinkId(e.getDocumentLinkId());
                    item.setLinkedDocumentId(e.getSourceDocumentId());
                    item.setLinkType(e.getLinkType());
                    item.setDirection("IN");
                    item.setCreatedAt(e.getCreatedAt());
                    item.setCreatedBy(e.getCreatedBy());
                    linkItems.add(item);
                }
            }
            response.setLinks(linkItems);
        } else {
            response.setLinks(null);
        }

        if (inc.assignments) {
            List<DocumentAssignmentEntity> assigns = documentAssignmentMapper.selectActiveByCompanyAndDocument(
                    companyId, documentId, relationLimit);
            List<DocumentEditorDetailResponse.AssignmentItem> aItems = new ArrayList<>();
            if (assigns != null) {
                for (DocumentAssignmentEntity r : assigns) {
                    DocumentEditorDetailResponse.AssignmentItem item = new DocumentEditorDetailResponse.AssignmentItem();
                    item.setDocumentAssignmentId(r.getDocumentAssignmentId());
                    item.setAssigneeUserId(r.getAssigneeUserId());
                    item.setAssignedByUserId(r.getAssignedByUserId());
                    item.setStatus(r.getStatus());
                    item.setAssignedAt(r.getAssignedAt());
                    aItems.add(item);
                }
            }
            response.setAssignments(aItems);
        } else {
            response.setAssignments(null);
        }

        if (inc.attachments) {
            List<DocumentAttachmentEntity> atts = documentAttachmentMapper.selectActiveByCompanyAndDocument(
                    companyId, documentId, relationLimit);
            List<DocumentEditorDetailResponse.AttachmentItem> attItems = new ArrayList<>();
            if (atts != null) {
                for (DocumentAttachmentEntity r : atts) {
                    DocumentEditorDetailResponse.AttachmentItem item = new DocumentEditorDetailResponse.AttachmentItem();
                    item.setDocumentAttachmentId(r.getDocumentAttachmentId());
                    item.setFileUrl(r.getFileUrl());
                    item.setFileName(r.getFileName());
                    item.setFileType(r.getFileType());
                    item.setFileSizeBytes(r.getFileSizeBytes());
                    item.setMediaKind(r.getMediaKind());
                    item.setUploadedAt(r.getUploadedAt());
                    item.setUploadedBy(r.getUploadedBy());
                    attItems.add(item);
                }
            }
            response.setAttachments(attItems);
        } else {
            response.setAttachments(null);
        }

        if (inc.accessRules) {
            List<DocumentAccessRuleEntity> ruleRows =
                    documentAccessRuleMapper.selectActiveByCompanyAndDocumentId(companyId, documentId);
            List<DocumentEditorDetailResponse.AccessRuleItem> ruleItems = new ArrayList<>();
            if (ruleRows != null) {
                for (DocumentAccessRuleEntity r : ruleRows) {
                    DocumentEditorDetailResponse.AccessRuleItem item = new DocumentEditorDetailResponse.AccessRuleItem();
                    item.setDocumentAccessRuleId(r.getDocumentAccessRuleId());
                    item.setRoleId(r.getRoleId());
                    item.setDepartmentId(r.getDepartmentId());
                    item.setStatus(r.getStatus());
                    item.setCreatedAt(r.getCreatedAt());
                    ruleItems.add(item);
                }
            }
            response.setAccessRules(ruleItems);
        } else {
            response.setAccessRules(null);
        }

        if (inc.blocks && documentBlockFeatureFlags.isReadEnabled()) {
            List<DocumentBlockEntity> blockRows = documentBlockMapper.selectActiveByCompanyAndDocumentId(companyId, documentId);
            List<DocumentEditorDetailResponse.BlockItem> blockItems = new ArrayList<>();
            if (blockRows != null) {
                for (DocumentBlockEntity b : blockRows) {
                    DocumentEditorDetailResponse.BlockItem item = new DocumentEditorDetailResponse.BlockItem();
                    item.setBlockId(b.getDocumentBlockId());
                    item.setParentBlockId(b.getParentBlockId());
                    item.setBlockType(b.getBlockType());
                    item.setProps(parseJson(b.getPropsJson()));
                    item.setContent(parseJson(b.getContentJson()));
                    item.setOrderKey(b.getOrderKey());
                    item.setCreatedAt(b.getCreatedAt());
                    item.setUpdatedAt(b.getUpdatedAt());
                    blockItems.add(item);
                }
            }
            response.setBlocks(blockItems);
        } else {
            response.setBlocks(null);
        }

        return response;
    }

    private JsonNode parseJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ex) {
            return null;
        }
    }
}
