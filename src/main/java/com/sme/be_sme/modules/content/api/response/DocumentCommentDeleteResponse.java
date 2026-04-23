package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentCommentDeleteResponse {
    private boolean deleted;
    private String documentId;
}
