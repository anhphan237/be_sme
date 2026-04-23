package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentPublishResponse {
    private String documentId;
    private Integer versionNo;
    private String documentVersionId;
}
