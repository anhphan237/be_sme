package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentAttachmentAddRequest {
    private String documentId;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSizeBytes;
    /** FILE or VIDEO */
    private String mediaKind;
}
