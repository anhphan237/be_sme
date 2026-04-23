package com.sme.be_sme.modules.document.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class DocumentLinkEntity {
    private String documentLinkId;
    private String companyId;
    private String sourceDocumentId;
    private String targetDocumentId;
    private String linkType;
    private String status;
    private String createdBy;
    private Date createdAt;
}
