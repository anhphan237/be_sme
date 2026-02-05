package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DocumentListResponse {
    private List<DocumentItem> items;

    @Getter
    @Setter
    public static class DocumentItem {
        private String documentId;
        private String name;
        private String fileUrl;
        private String description;
        private String status;
    }
}
