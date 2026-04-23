package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentAttachmentAddResponse {
    private String documentAttachmentId;
    private String documentId;
    private String mediaKind;
}
