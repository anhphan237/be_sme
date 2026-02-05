package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingTaskGenerateRequest {
    private String instanceId;
    /** For assignee resolution when template owner_type is MANAGER */
    private String managerId;
    /** For assignee resolution when template owner_type is IT_STAFF */
    private String itStaffUserId;
}
