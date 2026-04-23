package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class DocumentCommentUpdateResponse {
    private String commentId;
    private String documentId;
    private Date updatedAt;
}
