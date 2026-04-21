package com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventInstanceEntity {
    private String eventInstanceId;
    private String companyId;
    private String eventTemplateId;
    private Date eventDate;
    private String sourceType;
    private String sourceDepartmentIds;
    private String sourceUserIds;
    private String participantUserIds;
    private String status;
    private String createdBy;
    private Date createdAt;
    private Date updatedAt;
}
