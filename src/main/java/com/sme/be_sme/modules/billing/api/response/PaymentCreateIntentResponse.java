package com.sme.be_sme.modules.billing.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentCreateIntentResponse {
    /** Our payment transaction id */
    private String paymentTransactionId;
    /** Gateway payment intent id (e.g. Stripe pi_xxx) */
    private String paymentIntentId;
    /** Client secret for frontend to confirm payment (e.g. Stripe client_secret) */
    private String clientSecret;
    /** Gateway name (e.g. MOCK, STRIPE) */
    private String gateway;
    /** Amount in minor units (e.g. VND, cents) */
    private Integer amount;
    /** Currency code (e.g. VND, USD) */
    private String currency;
    /** Intent status (e.g. REQUIRES_PAYMENT_METHOD, REQUIRES_CONFIRMATION) */
    private String status;
    /** Invoice being paid */
    private String invoiceId;
}
