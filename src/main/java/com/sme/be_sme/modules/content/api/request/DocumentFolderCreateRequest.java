package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentFolderCreateRequest {
    private String name;
    private String parentFolderId;
}
