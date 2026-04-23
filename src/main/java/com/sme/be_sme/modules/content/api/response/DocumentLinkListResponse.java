package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class DocumentLinkListResponse {
    private String documentId;
    private List<LinkRow> items;

    @Getter
    @Setter
    public static class LinkRow {
        private String documentLinkId;
        private String linkedDocumentId;
        private String linkType;
        private String direction;
        private Date createdAt;
        private String createdBy;
    }
}
