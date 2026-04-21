package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventTemplateCreateRequest {
    private String name;
    private String content;
    private String description;
    private String status;
}
