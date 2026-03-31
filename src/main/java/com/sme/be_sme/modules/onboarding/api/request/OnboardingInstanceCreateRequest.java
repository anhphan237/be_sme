package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingInstanceCreateRequest {
    private String templateId;
    private String employeeId;
    /** users.user_id of line manager; stored on instance for task generation (MANAGER owner_type). */
    private String managerId;
    /** users.user_id of IT assignee for task generation (IT_STAFF owner_type). */
    private String itStaffUserId;
    /** Idempotency key: if provided and instance already exists with this requestNo, return existing. */
    private String requestNo;
}
