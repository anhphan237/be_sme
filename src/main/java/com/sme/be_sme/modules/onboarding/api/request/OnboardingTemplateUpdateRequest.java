package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingTemplateUpdateRequest {
    private String templateId;
    private String name;
    private String description;
    private String status;
}
