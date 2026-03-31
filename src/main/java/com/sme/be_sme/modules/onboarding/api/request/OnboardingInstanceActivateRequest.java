package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingInstanceActivateRequest {
    private String instanceId;
    /** Idempotency key: if provided and instance already ACTIVE with this requestNo, return existing. */
    private String requestNo;
    /** Optional override: persist to instance and use for task generation (MANAGER owner_type). */
    private String managerUserId;
    /** Optional override: persist to instance and use for task generation (IT_STAFF owner_type). */
    private String itStaffUserId;
}
