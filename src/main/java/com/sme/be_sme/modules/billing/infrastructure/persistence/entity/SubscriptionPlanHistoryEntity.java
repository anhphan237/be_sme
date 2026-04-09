package com.sme.be_sme.modules.billing.infrastructure.persistence.entity;

import java.util.Date;

public class SubscriptionPlanHistoryEntity {
    private String subscriptionPlanHistoryId;
    private String companyId;
    private String subscriptionId;
    private String oldPlanId;
    private String newPlanId;
    private String billingCycle;
    private String changedBy;
    private Date changedAt;
    private Date effectiveFrom;
    private Date effectiveTo;
    private Date createdAt;

    public String getSubscriptionPlanHistoryId() {
        return subscriptionPlanHistoryId;
    }

    public void setSubscriptionPlanHistoryId(String subscriptionPlanHistoryId) {
        this.subscriptionPlanHistoryId = subscriptionPlanHistoryId == null ? null : subscriptionPlanHistoryId.trim();
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId == null ? null : companyId.trim();
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId == null ? null : subscriptionId.trim();
    }

    public String getOldPlanId() {
        return oldPlanId;
    }

    public void setOldPlanId(String oldPlanId) {
        this.oldPlanId = oldPlanId == null ? null : oldPlanId.trim();
    }

    public String getNewPlanId() {
        return newPlanId;
    }

    public void setNewPlanId(String newPlanId) {
        this.newPlanId = newPlanId == null ? null : newPlanId.trim();
    }

    public String getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(String billingCycle) {
        this.billingCycle = billingCycle == null ? null : billingCycle.trim();
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy == null ? null : changedBy.trim();
    }

    public Date getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Date changedAt) {
        this.changedAt = changedAt;
    }

    public Date getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Date effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Date getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Date effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
