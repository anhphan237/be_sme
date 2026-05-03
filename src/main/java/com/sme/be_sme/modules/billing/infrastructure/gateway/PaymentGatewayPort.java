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
     * Default idempotency key is derived from {@code invoiceId} so duplicate FE calls do not create extra Stripe PaymentIntents.
     *
     * @param companyId   tenant id
     * @param invoiceId   invoice id (for idempotency / reference)
     * @param amountMinor amount in minor units (e.g. VND, cents)
     * @param currency    currency code (e.g. VND, USD)
     * @return result with paymentIntentId and clientSecret for frontend
     */
    default CreateIntentResult createIntent(String companyId, String invoiceId, Integer amountMinor, String currency) {
        String key = "payment-intent-" + (invoiceId != null ? invoiceId.trim() : "unknown");
        return createIntent(companyId, invoiceId, amountMinor, currency, key);
    }

    /**
     * @param idempotencyKey stable per invoice (max 255 for Stripe); passed as Idempotency-Key header for Stripe
     */
    CreateIntentResult createIntent(String companyId, String invoiceId, Integer amountMinor, String currency, String idempotencyKey);

    @Getter
    @Builder
    class CreateIntentResult {
        private final String gatewayName;
        private final String paymentIntentId;
        private final String clientSecret;
        private final String status;
    }
}
