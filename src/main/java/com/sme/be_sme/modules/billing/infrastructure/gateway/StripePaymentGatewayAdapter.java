package com.sme.be_sme.modules.billing.infrastructure.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
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

/**
 * Stripe payment gateway adapter. Creates PaymentIntent via Stripe API.
 * Set app.payment.gateway=stripe and app.stripe.secret-key=sk_xxx to enable.
 * Does NOT fallback to mock - throws AppException when Stripe is unavailable.
 */
@Component
@ConditionalOnProperty(name = "app.payment.gateway", havingValue = "stripe")
@RequiredArgsConstructor
@Slf4j
public class StripePaymentGatewayAdapter implements PaymentGatewayPort {

    private static final String STRIPE_API = "https://api.stripe.com/v1/payment_intents";
    /** Stripe min ~50 cents USD; for VND use 20,000 to be safe */
    private static final long MIN_AMOUNT_VND = 20_000;

    @Value("${app.stripe.secret-key:}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public CreateIntentResult createIntent(String companyId, String invoiceId, Integer amountMinor, String currency) {
        if (!StringUtils.hasText(secretKey)) {
            log.error("Stripe secret key not set - cannot create payment intent. Set STRIPE_SECRET_KEY env var.");
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "Stripe is not configured. Set STRIPE_SECRET_KEY.");
        }
        String currencyLower = currency == null ? "vnd" : currency.trim().toLowerCase();
        long amount = amountMinor == null ? 0 : amountMinor.longValue();
        if (amount <= 0) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "Invoice amount must be positive");
        }
        if ("vnd".equals(currencyLower) && amount < MIN_AMOUNT_VND) {
            throw AppException.of(ErrorCodes.BAD_REQUEST,
                    "Amount must be at least " + MIN_AMOUNT_VND + " VND (Stripe minimum)");
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
                if (!StringUtils.hasText(clientSecret)) {
                    log.error("Stripe returned no client_secret for invoice {}", invoiceId);
                    throw AppException.of(ErrorCodes.INTERNAL_ERROR, "Stripe did not return client secret");
                }
                return CreateIntentResult.builder()
                        .gatewayName("stripe")
                        .paymentIntentId(piId)
                        .clientSecret(clientSecret)
                        .status(status)
                        .build();
            }
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "Stripe API returned " + response.getStatusCode());
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Stripe createIntent failed for invoice {}: {}", invoiceId, e.getMessage(), e);
            throw AppException.of(ErrorCodes.INTERNAL_ERROR,
                    "Stripe payment failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }
}
