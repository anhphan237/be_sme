package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class DocumentCommentAddResponse {
    private String commentId;
    private String documentId;
    private String parentCommentId;
    private String anchorBlockId;
    private Integer anchorStart;
    private Integer anchorEnd;
    private String anchorText;
    private Date createdAt;
}
