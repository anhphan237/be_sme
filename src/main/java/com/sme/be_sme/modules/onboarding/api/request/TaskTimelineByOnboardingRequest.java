package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskTimelineByOnboardingRequest {
    private String onboardingId;
    /** When false (default), excludes DONE tasks to show only actionable work. */
    private Boolean includeDone;
}

