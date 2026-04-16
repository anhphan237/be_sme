package com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskRequiredDocumentEntity {
    private String taskRequiredDocumentId;
    private String companyId;
    private String taskId;
    private String documentId;
    private Date createdAt;
}
