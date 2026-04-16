package com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskTemplateRequiredDocumentEntity {
    private String taskTemplateRequiredDocumentId;
    private String companyId;
    private String taskTemplateId;
    private String documentId;
    private Date createdAt;
}
