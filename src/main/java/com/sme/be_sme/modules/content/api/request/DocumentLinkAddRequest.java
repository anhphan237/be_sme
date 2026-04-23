package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentLinkAddRequest {
    private String sourceDocumentId;
    private String targetDocumentId;
    /** e.g. RELATED, SEE_ALSO; defaults server-side if blank */
    private String linkType;
}
