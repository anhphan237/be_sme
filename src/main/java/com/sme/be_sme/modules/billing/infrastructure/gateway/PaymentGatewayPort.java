package com.sme.be_sme.modules.billing.infrastructure.gateway;

import lombok.Builder;
import lombok.Getter;

/**
 * Port for payment gateway (one gateway per deployment).
 * Create a payment intent so the client can complete payment (e.g. Stripe PaymentIntent).
 */
public interface PaymentGatewayPort {

    String GATEWAY_MOCK = "MOCK";

    /**
     * Create a payment intent for the given invoice amount.
     *
     * @param companyId   tenant id
     * @param invoiceId   invoice id (for idempotency / reference)
     * @param amountMinor amount in minor units (e.g. VND, cents)
     * @param currency    currency code (e.g. VND, USD)
     * @return result with paymentIntentId and clientSecret for frontend
     */
    CreateIntentResult createIntent(String companyId, String invoiceId, Integer amountMinor, String currency);

    @Getter
    @Builder
    class CreateIntentResult {
        private final String gatewayName;
        private final String paymentIntentId;
        private final String clientSecret;
        private final String status;
    }
}
