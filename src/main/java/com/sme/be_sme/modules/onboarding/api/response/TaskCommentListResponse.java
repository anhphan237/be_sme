package com.sme.be_sme.modules.onboarding.api.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskCommentListResponse {
    private String taskId;
    /** Same shape as {@link TaskDetailResponse#getComments()}. */
    private List<TaskDetailResponse.CommentItem> comments;
}
