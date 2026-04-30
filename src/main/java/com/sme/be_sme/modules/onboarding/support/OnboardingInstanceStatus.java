package com.sme.be_sme.modules.onboarding.support;

public final class OnboardingInstanceStatus {

    public static final String DRAFT = "DRAFT";
    public static final String ACTIVE = "ACTIVE";
    public static final String DONE = "DONE";
    public static final String CANCELLED = "CANCELLED";

    /** Backward compatibility for legacy data/logic. */
    public static final String COMPLETED_LEGACY = "COMPLETED";

    private OnboardingInstanceStatus() {
    }
}

