package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentFolderMoveResponse {
    private String folderId;
    private String parentFolderId;
}
