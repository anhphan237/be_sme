package com.sme.be_sme.modules.billing.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * Plan usage timeline: each segment is the period when {@link Segment#getPlanCode()} was active
 * (after a subscription created / plan change), from {@code effectiveFrom} to {@code effectiveTo}
 * (null end means still active).
 */
@Getter
@Setter
public class SubscriptionPlanTimelineResponse {
    private List<Segment> segments;
    /** total segments matching filter (all pages) */
    private Integer total;
    private Integer page;
    private Integer size;
    private Integer totalPages;

    @Getter
    @Setter
    public static class Segment {
        private String subscriptionId;
        private String planId;
        private String planCode;
        private String planName;
        private String billingCycle;
        private Date effectiveFrom;
        /** null = ongoing (current plan segment) */
        private Date effectiveTo;
        private String historyId;
        /** User id who caused this segment (same as subscription_plan_history.changed_by) */
        private String changedBy;
        /** Display name for {@link #changedBy}; null if not a tenant user or unknown */
        private String changedByName;
        /** When the plan change was recorded */
        private Date changedAt;
    }
}
