package com.sme.be_sme.modules.onboarding.support;

/**
 * FE contract (task lifecycle):
 * <ul>
 *   <li>{@link #STATUS_PENDING_APPROVAL} — employee submits for manager review when
 *       {@code requiresManagerApproval} is true; use instead of {@code DONE}.</li>
 *   <li>{@code DONE} — final state; employees may set only when approval is not required (after optional ack).</li>
 *   <li>{@code approvalStatus}: {@link #APPROVAL_NONE} (default), {@link #APPROVAL_PENDING},
 *       {@link #APPROVAL_APPROVED}, {@link #APPROVAL_REJECTED}.</li>
 *   <li>ACK: call {@code com.sme.onboarding.task.acknowledge} before {@code DONE} when {@code requireAck} is true;
 *       response status becomes {@link #STATUS_WAIT_ACK} (ack recorded; use {@code updateStatus} to {@code DONE}).</li>
 *   <li>Manager: {@code com.sme.onboarding.task.approve} / {@code .reject} when status is {@link #STATUS_PENDING_APPROVAL}.</li>
 * </ul>
 */
public final class OnboardingTaskWorkflow {

    public static final String APPROVAL_NONE = "NONE";
    public static final String APPROVAL_PENDING = "PENDING";
    public static final String APPROVAL_APPROVED = "APPROVED";
    public static final String APPROVAL_REJECTED = "REJECTED";

    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";

    /** Set after {@code acknowledge} when {@code requireAck}; employee then calls {@code updateStatus} → {@code DONE}. */
    public static final String STATUS_WAIT_ACK = "WAIT_ACK";

    private OnboardingTaskWorkflow() {}
}
