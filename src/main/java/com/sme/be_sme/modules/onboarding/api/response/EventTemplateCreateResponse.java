package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventTemplateCreateResponse {
    private String eventTemplateId;
    private String name;
    private String status;
}
