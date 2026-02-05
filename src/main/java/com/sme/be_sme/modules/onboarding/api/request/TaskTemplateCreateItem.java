package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskTemplateCreateItem {
    private String title;
    private String description;
    private String ownerType;
    private String ownerRefId;
    private Integer dueDaysOffset;
    private Boolean requireAck;
    private Integer sortOrder;
    private String status;
}
