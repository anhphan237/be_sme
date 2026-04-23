package com.sme.be_sme.modules.document.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class DocumentCommentEntity {
    private String documentCommentId;
    private String companyId;
    private String documentId;
    private String parentCommentId;
    private String authorUserId;
    private String body;
    private String status;
    private Date createdAt;
    private Date updatedAt;
}
