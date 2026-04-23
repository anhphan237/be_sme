package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentCreateDraftRequest {
    private String title;
    private String documentCategoryId;
    /** JSON string; if blank, defaults to {} */
    private String draftJson;
}
