package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingInstanceCreateRequest {
    private String templateId;
    private String employeeId;
    private String managerId; // optional
}
