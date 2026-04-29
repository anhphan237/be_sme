package com.sme.be_sme.modules.content.api.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class DocumentEditorDetailResponse {
    private String documentId;
    private String title;
    private String description;
    private String status;
    private String contentKind;
    private JsonNode draftContent;
    private JsonNode publishedContent;
    private Date publishedAt;
    private String publishedBy;
    private Date createdAt;
    private Date updatedAt;
    private List<ActivityItem> activity;
    private List<ReadItem> reads;
    /** EDITOR doc folder placement; null if not in a folder */
    private FolderPlacement folderPlacement;
    /** Flat list with parentCommentId for threading on FE */
    private List<CommentItem> comments;
    /** Phase 3: cross-doc links (direction OUT|IN on each item) */
    private List<LinkItem> links;
    private List<AssignmentItem> assignments;
    private List<AttachmentItem> attachments;
    private List<BlockItem> blocks;
    /** Present when {@code include} contains {@code accessRules} */
    private List<AccessRuleItem> accessRules;

    @Getter
    @Setter
    public static class ActivityItem {
        private String action;
        private String actorUserId;
        private JsonNode detail;
        private Date createdAt;
    }

    @Getter
    @Setter
    public static class ReadItem {
        private String userId;
        private String status;
        private Date readAt;
        private Date ackedAt;
    }

    @Getter
    @Setter
    public static class FolderPlacement {
        private String folderId;
        private String folderName;
        private List<String> path;
    }

    @Getter
    @Setter
    public static class CommentItem {
        private String commentId;
        private String parentCommentId;
        private String anchorBlockId;
        private Integer anchorStart;
        private Integer anchorEnd;
        private String anchorText;
        private String authorUserId;
        private String body;
        private String status;
        private Date createdAt;
        private Date updatedAt;
    }

    @Getter
    @Setter
    public static class LinkItem {
        private String documentLinkId;
        private String linkedDocumentId;
        private String linkType;
        private String direction;
        private Date createdAt;
        private String createdBy;
    }

    @Getter
    @Setter
    public static class AssignmentItem {
        private String documentAssignmentId;
        private String assigneeUserId;
        private String assignedByUserId;
        private String status;
        private Date assignedAt;
    }

    @Getter
    @Setter
    public static class AttachmentItem {
        private String documentAttachmentId;
        private String fileUrl;
        private String fileName;
        private String fileType;
        private Long fileSizeBytes;
        private String mediaKind;
        private Date uploadedAt;
        private String uploadedBy;
    }

    @Getter
    @Setter
    public static class BlockItem {
        private String blockId;
        private String parentBlockId;
        private String blockType;
        private JsonNode props;
        private JsonNode content;
        private String orderKey;
        private Date createdAt;
        private Date updatedAt;
    }

    @Getter
    @Setter
    public static class AccessRuleItem {
        private String documentAccessRuleId;
        private String roleId;
        private String departmentId;
        private String status;
        private Date createdAt;
    }
}
