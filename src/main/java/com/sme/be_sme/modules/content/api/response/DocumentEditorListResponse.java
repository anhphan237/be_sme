package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class DocumentEditorListResponse {
    private List<Item> items;
    private Long totalCount;
    private Integer page;
    private Integer pageSize;

    @Getter
    @Setter
    public static class Item {
        private String documentId;
        private String title;
        private String status;
        private String contentKind;
        private Date updatedAt;
        private Boolean published;
    }
}
