package com.sme.be_sme.modules.content.api.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentBlockCreateRequest {
    private String documentId;
    private String parentBlockId;
    private String blockType;
    private JsonNode props;
    private JsonNode content;
    private String afterBlockId;
}
