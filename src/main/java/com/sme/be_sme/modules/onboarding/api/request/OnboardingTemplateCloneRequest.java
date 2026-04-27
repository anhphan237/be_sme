package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingTemplateCloneRequest {
    private String sourceTemplateId;
    private String level;
    private String name;
    private String description;
    private String status;
    private String createdBy;
}
