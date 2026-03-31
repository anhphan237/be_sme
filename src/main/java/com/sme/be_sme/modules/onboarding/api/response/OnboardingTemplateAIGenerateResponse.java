package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingTemplateAIGenerateResponse {
    private String templateId;
    private String name;
    private int totalChecklists;
    private int totalTasks;
}
