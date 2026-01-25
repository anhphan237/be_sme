package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingTemplateResponse {
    private String templateId;
    private String name;
    private String status;
}
