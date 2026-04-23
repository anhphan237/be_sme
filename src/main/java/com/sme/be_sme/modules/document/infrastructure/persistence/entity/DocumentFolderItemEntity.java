package com.sme.be_sme.modules.document.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class DocumentFolderItemEntity {
    private String documentFolderItemId;
    private String companyId;
    private String folderId;
    private String documentId;
    private Date createdAt;
}
