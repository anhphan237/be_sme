package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentCommentUpdateRequest {
    private String commentId;
    private String body;
}
