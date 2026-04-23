package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentLinkListRequest {
    private String documentId;
    /** OUT | IN | BOTH — default OUT */
    private String direction;
    private Integer limit;
}
