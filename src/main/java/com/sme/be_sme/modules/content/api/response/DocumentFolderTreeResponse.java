package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class DocumentFolderTreeResponse {
    /** Root folders (parentFolderId null or missing parent), each with nested children */
    private List<FolderTreeNode> roots;

    @Getter
    @Setter
    public static class FolderTreeNode {
        private String folderId;
        private String parentFolderId;
        private String name;
        private Integer sortOrder;
        private Date createdAt;
        private List<FolderTreeNode> children = new ArrayList<>();
    }
}
