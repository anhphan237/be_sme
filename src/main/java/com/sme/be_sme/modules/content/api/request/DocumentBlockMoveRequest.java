package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentBlockMoveRequest {
    private String documentId;
    private String blockId;
    private String parentBlockId;
    private String afterBlockId;
}
