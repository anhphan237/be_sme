package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class DocumentFolderTreeWithDocumentsResponse {
    private List<FolderNode> roots;

    @Getter
    @Setter
    public static class FolderNode {
        private String folderId;
        private String parentFolderId;
        private String name;
        private Integer sortOrder;
        private Date createdAt;
        private List<FolderNode> children = new ArrayList<>();
        private List<DocumentNode> documents = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class DocumentNode {
        private String documentId;
        private String title;
        private String status;
        private Date updatedAt;
        private Boolean published;
    }
}
