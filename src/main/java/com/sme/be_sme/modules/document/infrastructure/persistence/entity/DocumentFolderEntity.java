package com.sme.be_sme.modules.document.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class DocumentFolderEntity {
    private String folderId;
    private String companyId;
    private String parentFolderId;
    private String name;
    private Integer sortOrder;
    private String status;
    private String createdBy;
    private Date createdAt;
    private Date updatedAt;
}
