package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class DocumentFolderListResponse {
    private List<FolderItem> items;

    @Getter
    @Setter
    public static class FolderItem {
        private String folderId;
        private String parentFolderId;
        private String name;
        private Integer sortOrder;
        private Date createdAt;
    }
}
