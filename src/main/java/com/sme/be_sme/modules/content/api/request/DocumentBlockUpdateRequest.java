package com.sme.be_sme.modules.content.api.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentBlockUpdateRequest {
    private String documentId;
    private String blockId;
    private JsonNode props;
    private JsonNode content;
    private String blockType;
}
