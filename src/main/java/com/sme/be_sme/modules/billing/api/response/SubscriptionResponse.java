package com.sme.be_sme.modules.billing.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class SubscriptionResponse {
    private String subscriptionId;
    private String planCode;
    private String status;
    private String billingCycle;
    private Date currentPeriodStart;
    private Date currentPeriodEnd;
    private Boolean autoRenew;
    /** Prorated credit (VND) when downgrading – apply to next invoice or refund */
    private Integer prorateCreditVnd;
    /** Prorated charge (VND) when upgrading – charge or add to next invoice */
    private Integer prorateChargeVnd;
    /** true when plan change is created but waiting for payment success */
    private Boolean paymentRequired;
    /** pending subscription change request id (if paymentRequired=true) */
    private String pendingChangeId;
    /** issued invoice id for completing pending plan change payment */
    private String paymentInvoiceId;
    /** requested plan code waiting to be applied after successful payment */
    private String pendingPlanCode;
    /** requested billing cycle waiting to be applied after successful payment */
    private String pendingBillingCycle;
}
