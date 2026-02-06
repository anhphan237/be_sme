package com.sme.be_sme.modules.billing.infrastructure.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.sme.be_sme.modules.billing.infrastructure.gateway.PaymentGatewayPort.CreateIntentResult;

/**
 * Mock payment gateway for development. Used when app.payment.gateway is not "stripe".
 */
@Component
@ConditionalOnProperty(name = "app.payment.gateway", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGatewayAdapter implements PaymentGatewayPort {

    @Override
    public CreateIntentResult createIntent(String companyId, String invoiceId, Integer amountMinor, String currency) {
        String fakeId = "pi_mock_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        String clientSecret = "mock_secret_" + fakeId;
        return CreateIntentResult.builder()
                .gatewayName(PaymentGatewayPort.GATEWAY_MOCK)
                .paymentIntentId(fakeId)
                .clientSecret(clientSecret)
                .status("REQUIRES_PAYMENT_METHOD")
                .build();
    }
}
