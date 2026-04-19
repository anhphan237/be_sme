package com.sme.be_sme.modules.onboarding.api.request;

import java.util.Date;
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
    /** First calendar day of onboarding; when null, keeps existing {@code start_date} or defaults to today. */
    private Date expectedStartDate;
}
