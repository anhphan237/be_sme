package com.sme.be_sme.modules.document.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class DocumentBlockEntity {
    private String documentBlockId;
    private String companyId;
    private String documentId;
    private String parentBlockId;
    private String blockType;
    private String propsJson;
    private String contentJson;
    private String orderKey;
    private String status;
    private String createdBy;
    private Date createdAt;
    private Date updatedAt;
}
