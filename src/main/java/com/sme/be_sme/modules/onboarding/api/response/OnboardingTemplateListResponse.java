package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Data;

import java.util.List;

@Data
public class OnboardingTemplateListResponse {
    private List<OnboardingTemplateResponse> templates;
}
