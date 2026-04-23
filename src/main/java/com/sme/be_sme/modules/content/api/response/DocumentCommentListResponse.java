package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class DocumentCommentListResponse {
    private String documentId;
    private List<CommentRow> items;

    @Getter
    @Setter
    public static class CommentRow {
        private String commentId;
        private String parentCommentId;
        private String authorUserId;
        private String body;
        private String status;
        private Date createdAt;
        private Date updatedAt;
    }
}
