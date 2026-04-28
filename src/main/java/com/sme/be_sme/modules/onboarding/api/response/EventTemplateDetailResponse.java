package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class EventTemplateDetailResponse {
    private String eventTemplateId;
    private String name;
    private String content;
    private String description;
    private String status;
    private String createdBy;
    private Date createdAt;
    private Date updatedAt;
}
