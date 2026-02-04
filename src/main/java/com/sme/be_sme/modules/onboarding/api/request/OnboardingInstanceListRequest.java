package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingInstanceListRequest {
    private String employeeId; // optional
    private String status; // optional
}
