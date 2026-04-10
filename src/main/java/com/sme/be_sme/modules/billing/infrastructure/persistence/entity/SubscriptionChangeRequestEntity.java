package com.sme.be_sme.modules.billing.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class SubscriptionChangeRequestEntity {
    private String subscriptionChangeRequestId;
    private String companyId;
    private String subscriptionId;
    private String oldPlanId;
    private String newPlanId;
    private String billingCycle;
    private String invoiceId;
    private String status;
    private String requestedBy;
    private String failureReason;
    private Date requestedAt;
    private Date appliedAt;
    private Date createdAt;
    private Date updatedAt;
}
