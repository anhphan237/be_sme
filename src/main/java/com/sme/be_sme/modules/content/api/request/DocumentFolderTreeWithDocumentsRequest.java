package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentFolderTreeWithDocumentsRequest {
    /** Optional title filter applied to document nodes only */
    private String titleQuery;
}
