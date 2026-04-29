package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentCommentAddRequest {
    private String documentId;
    private String body;
    private String parentCommentId;
    private String anchorBlockId;
    private Integer anchorStart;
    private Integer anchorEnd;
    private String anchorText;
}
