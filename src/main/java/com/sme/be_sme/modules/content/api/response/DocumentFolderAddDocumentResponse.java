package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentFolderAddDocumentResponse {
    private String documentFolderItemId;
    private String folderId;
    private String documentId;
}
