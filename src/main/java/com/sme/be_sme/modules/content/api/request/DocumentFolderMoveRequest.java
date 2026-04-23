package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentFolderMoveRequest {
    private String folderId;
    /** null or blank = move to root */
    private String newParentFolderId;
}
