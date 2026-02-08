package com.sme.be_sme.modules.billing.infrastructure.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import static com.sme.be_sme.modules.billing.infrastructure.gateway.PaymentGatewayPort.CreateIntentResult;
import static com.sme.be_sme.modules.billing.infrastructure.gateway.PaymentGatewayPort.GATEWAY_MOCK;

/**
 * Stripe payment gateway adapter. Creates PaymentIntent via Stripe API.
 * Set app.payment.gateway=stripe and app.stripe.secret-key=sk_xxx to enable.
 */
@Component
@ConditionalOnProperty(name = "app.payment.gateway", havingValue = "stripe")
@RequiredArgsConstructor
@Slf4j
public class StripePaymentGatewayAdapter implements PaymentGatewayPort {

    private static final String STRIPE_API = "https://api.stripe.com/v1/payment_intents";

    @Value("${app.stripe.secret-key:}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public CreateIntentResult createIntent(String companyId, String invoiceId, Integer amountMinor, String currency) {
        if (!StringUtils.hasText(secretKey)) {
            log.warn("Stripe secret key not set; returning mock result");
            return mockResult(invoiceId);
        }
        String currencyLower = currency == null ? "vnd" : currency.trim().toLowerCase();
        // Stripe amount is in cents (smallest unit). For VND, amount is already in VND units.
        long amount = amountMinor == null ? 0 : amountMinor.longValue();
        if (amount <= 0) {
            return CreateIntentResult.builder()
                    .gatewayName("stripe")
                    .paymentIntentId(null)
                    .clientSecret(null)
                    .status("invalid_amount")
                    .build();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBearerAuth(secretKey.trim());

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("amount", String.valueOf(amount));
            body.add("currency", currencyLower);
            body.add("metadata[invoice_id]", invoiceId == null ? "" : invoiceId);
            body.add("metadata[company_id]", companyId == null ? "" : companyId);
            body.add("automatic_payment_methods[enabled]", "true");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(STRIPE_API, HttpMethod.POST, request, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String piId = root.has("id") ? root.path("id").asText() : null;
                String clientSecret = root.has("client_secret") ? root.path("client_secret").asText() : null;
                String status = root.has("status") ? root.path("status").asText() : "unknown";
                return CreateIntentResult.builder()
                        .gatewayName("stripe")
                        .paymentIntentId(piId)
                        .clientSecret(clientSecret)
                        .status(status)
                        .build();
            }
        } catch (Exception e) {
            log.warn("Stripe createIntent failed: {}", e.getMessage());
        }
        return mockResult(invoiceId);
    }

    private CreateIntentResult mockResult(String invoiceId) {
        String fakeId = "pi_mock_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        return CreateIntentResult.builder()
                .gatewayName(GATEWAY_MOCK)
                .paymentIntentId(fakeId)
                .clientSecret("mock_secret_" + fakeId)
                .status("REQUIRES_PAYMENT_METHOD")
                .build();
    }
}
