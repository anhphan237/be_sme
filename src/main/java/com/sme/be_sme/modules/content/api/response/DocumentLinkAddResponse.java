package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentLinkAddResponse {
    private String documentLinkId;
    private String sourceDocumentId;
    private String targetDocumentId;
    private String linkType;
}
