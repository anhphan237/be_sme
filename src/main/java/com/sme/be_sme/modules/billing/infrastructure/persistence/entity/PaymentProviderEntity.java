package com.sme.be_sme.modules.billing.infrastructure.persistence.entity;

import java.util.Date;

public class PaymentProviderEntity {
    private String paymentProviderId;
    private String companyId;
    private String providerName;
    private String status;
    private String accountId;
    private Date lastSyncAt;
    private Date createdAt;
    private Date updatedAt;

    public String getPaymentProviderId() { return paymentProviderId; }
    public void setPaymentProviderId(String paymentProviderId) { this.paymentProviderId = paymentProviderId == null ? null : paymentProviderId.trim(); }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId == null ? null : companyId.trim(); }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName == null ? null : providerName.trim(); }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status == null ? null : status.trim(); }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId == null ? null : accountId.trim(); }

    public Date getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(Date lastSyncAt) { this.lastSyncAt = lastSyncAt; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
