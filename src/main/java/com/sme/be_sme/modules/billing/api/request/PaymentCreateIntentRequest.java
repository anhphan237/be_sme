package com.sme.be_sme.modules.billing.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentCreateIntentRequest {
    /** Invoice to pay (required) */
    private String invoiceId;
}
