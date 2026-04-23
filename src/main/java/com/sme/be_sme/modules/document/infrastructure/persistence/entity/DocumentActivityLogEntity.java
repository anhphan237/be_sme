package com.sme.be_sme.modules.document.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class DocumentActivityLogEntity {
    private String documentActivityLogId;
    private String companyId;
    private String documentId;
    private String action;
    private String actorUserId;
    private String detailJson;
    private Date createdAt;
}
