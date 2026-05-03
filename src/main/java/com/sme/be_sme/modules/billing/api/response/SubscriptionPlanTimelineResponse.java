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
    private Integer total;

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
    }
}
