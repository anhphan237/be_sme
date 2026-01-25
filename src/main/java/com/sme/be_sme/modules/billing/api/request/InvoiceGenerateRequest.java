package com.sme.be_sme.modules.billing.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceGenerateRequest {
    private String subscriptionId;
    private String periodStart;
    private String periodEnd;
}
