package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentUpdateDraftRequest {
    private String documentId;
    private String title;
    private String draftJson;
}
