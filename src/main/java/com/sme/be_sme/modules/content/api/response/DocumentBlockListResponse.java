package com.sme.be_sme.modules.content.api.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class DocumentBlockListResponse {
    private String documentId;
    private List<BlockItem> items;

    @Getter
    @Setter
    public static class BlockItem {
        private String blockId;
        private String parentBlockId;
        private String blockType;
        private JsonNode props;
        private JsonNode content;
        private String orderKey;
        private Date createdAt;
        private Date updatedAt;
    }
}
