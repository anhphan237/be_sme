package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class TaskCommentTreeResponse {
    private String taskId;
    private List<CommentNode> roots;

    @Getter
    @Setter
    public static class CommentNode {
        private String commentId;
        private String parentCommentId;
        private String content;
        private String createdBy;
        private String createdByName;
        private Date createdAt;
        private List<CommentNode> children = new ArrayList<>();
    }
}
