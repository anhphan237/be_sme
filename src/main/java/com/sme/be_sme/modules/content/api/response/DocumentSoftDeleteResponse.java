package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentSoftDeleteResponse {
    private String documentId;
    private boolean deleted;
}
