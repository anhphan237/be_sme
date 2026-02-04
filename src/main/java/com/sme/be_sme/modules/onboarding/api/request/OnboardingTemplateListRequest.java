package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Data;

@Data
public class OnboardingTemplateListRequest {
    // optional, default ACTIVE
    private String status;
}
