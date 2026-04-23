package com.sme.be_sme.modules.document.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class DocumentAssignmentEntity {
    private String documentAssignmentId;
    private String companyId;
    private String documentId;
    private String assigneeUserId;
    private String assignedByUserId;
    private String status;
    private Date assignedAt;
    private Date updatedAt;
}
