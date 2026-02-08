package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentListRequest {
    /** Optional: filter by category */
    private String documentCategoryId;
}
