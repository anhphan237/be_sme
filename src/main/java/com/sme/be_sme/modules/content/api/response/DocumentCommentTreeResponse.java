package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class DocumentCommentTreeResponse {
    private String documentId;
    private List<CommentNode> roots;

    @Getter
    @Setter
    public static class CommentNode {
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
        private List<CommentNode> children = new ArrayList<>();
    }
}
