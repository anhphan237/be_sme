package com.sme.be_sme.modules.document.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class DocumentAttachmentEntity {
    private String documentAttachmentId;
    private String companyId;
    private String documentId;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSizeBytes;
    private String mediaKind;
    private String status;
    private String uploadedBy;
    private Date uploadedAt;
}
