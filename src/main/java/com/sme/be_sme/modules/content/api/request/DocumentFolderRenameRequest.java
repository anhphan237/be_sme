package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentFolderRenameRequest {
    private String folderId;
    private String name;
}
