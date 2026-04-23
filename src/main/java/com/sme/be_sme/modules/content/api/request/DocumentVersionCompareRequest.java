package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentVersionCompareRequest {
    private String documentId;
    private String fromDocumentVersionId;
    private String toDocumentVersionId;
}
