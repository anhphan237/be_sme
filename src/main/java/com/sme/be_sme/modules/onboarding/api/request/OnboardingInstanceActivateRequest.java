package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingInstanceActivateRequest {
    private String instanceId;
    /** Idempotency key: if provided and instance already ACTIVE with this requestNo, return existing. */
    private String requestNo;
}
