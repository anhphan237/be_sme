package com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventTemplateEntity {
    private String eventTemplateId;
    private String companyId;
    private String name;
    private String content;
    private String description;
    private String status;
    private String createdBy;
    private Date createdAt;
    private Date updatedAt;
}
