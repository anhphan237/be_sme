package com.sme.be_sme.modules.onboarding.support;

import java.util.Locale;
import java.util.Set;

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

    public static final String STATUS_TODO = "TODO";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_DONE = "DONE";
    public static final String APPROVAL_NONE = "NONE";
    public static final String APPROVAL_PENDING = "PENDING";
    public static final String APPROVAL_APPROVED = "APPROVED";
    public static final String APPROVAL_REJECTED = "REJECTED";

    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";

    /** Set after {@code acknowledge} when {@code requireAck}; employee then calls {@code updateStatus} → {@code DONE}. */
    public static final String STATUS_WAIT_ACK = "WAIT_ACK";
    public static final String SCHEDULE_UNSCHEDULED = "UNSCHEDULED";
    public static final String SCHEDULE_PROPOSED = "PROPOSED";
    public static final String SCHEDULE_CONFIRMED = "CONFIRMED";
    public static final String SCHEDULE_RESCHEDULED = "RESCHEDULED";
    public static final String SCHEDULE_CANCELLED = "CANCELLED";
    public static final String SCHEDULE_MISSED = "MISSED";

    private static final Set<String> KNOWN_STATUSES = Set.of(
            STATUS_TODO,
            STATUS_IN_PROGRESS,
            STATUS_ASSIGNED,
            STATUS_WAIT_ACK,
            STATUS_PENDING_APPROVAL,
            STATUS_DONE
    );

    private OnboardingTaskWorkflow() {}

    public static String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("PENDING".equals(normalized)) {
            return STATUS_PENDING_APPROVAL;
        }
        return normalized;
    }

    public static boolean isKnownStatus(String status) {
        String normalized = normalizeStatus(status);
        return normalized != null && KNOWN_STATUSES.contains(normalized);
    }

    public static boolean canAcknowledgeFrom(String status) {
        String normalized = normalizeStatus(status);
        if (normalized == null) {
            return false;
        }
        return STATUS_TODO.equals(normalized)
                || STATUS_IN_PROGRESS.equals(normalized)
                || STATUS_ASSIGNED.equals(normalized)
                || STATUS_WAIT_ACK.equals(normalized);
    }

    public static boolean canTransition(String currentStatus, String targetStatus) {
        String current = normalizeStatus(currentStatus);
        String target = normalizeStatus(targetStatus);
        if (target == null || !isKnownStatus(target)) {
            return false;
        }
        if (current == null) {
            return true;
        }
        if (current.equals(target)) {
            return true;
        }
        switch (current) {
            case STATUS_TODO:
                return STATUS_IN_PROGRESS.equals(target)
                        || STATUS_ASSIGNED.equals(target)
                        || STATUS_WAIT_ACK.equals(target)
                        || STATUS_PENDING_APPROVAL.equals(target)
                        || STATUS_DONE.equals(target);
            case STATUS_IN_PROGRESS:
                return STATUS_TODO.equals(target)
                        || STATUS_ASSIGNED.equals(target)
                        || STATUS_WAIT_ACK.equals(target)
                        || STATUS_PENDING_APPROVAL.equals(target)
                        || STATUS_DONE.equals(target);
            case STATUS_ASSIGNED:
                return STATUS_TODO.equals(target)
                        || STATUS_IN_PROGRESS.equals(target)
                        || STATUS_WAIT_ACK.equals(target)
                        || STATUS_PENDING_APPROVAL.equals(target)
                        || STATUS_DONE.equals(target);
            case STATUS_WAIT_ACK:
                return STATUS_TODO.equals(target)
                        || STATUS_IN_PROGRESS.equals(target)
                        || STATUS_ASSIGNED.equals(target)
                        || STATUS_DONE.equals(target);
            case STATUS_PENDING_APPROVAL:
                return STATUS_TODO.equals(target) || STATUS_DONE.equals(target);
            case STATUS_DONE:
                return false;
            default:
                return false;
        }
    }
}
