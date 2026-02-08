package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentUploadResponse {
    private String documentId;
    private String name;
    private String fileUrl;
    private String description;
}
