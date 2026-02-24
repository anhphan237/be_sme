package com.sme.be_sme.modules.billing.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentStatusResponse {
    private String id;
    private String clientSecret;
    private Integer amount;
    private String currency;
    private String status;
    private String invoiceId;
}
