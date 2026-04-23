package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentFolderCreateResponse {
    private String folderId;
    private String name;
    private String parentFolderId;
}
