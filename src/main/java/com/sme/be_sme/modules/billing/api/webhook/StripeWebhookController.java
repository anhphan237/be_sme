package com.sme.be_sme.modules.billing.api.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.enums.InvoiceStatus;
import com.sme.be_sme.modules.billing.enums.PaymentTransactionStatus;
import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PaymentTransactionMapperExt;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PaymentTransactionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final ObjectMapper objectMapper;
    private final PaymentTransactionMapperExt paymentTransactionMapperExt;
    private final InvoiceMapper invoiceMapper;

    @Value("${app.stripe.webhook-secret:}")
    private String webhookSecret;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature) {

        if (StringUtils.hasText(webhookSecret) && StringUtils.hasText(stripeSignature)) {
            if (!verifySignature(payload, stripeSignature, webhookSecret.trim())) {
                log.warn("Stripe webhook signature verification failed");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid signature");
            }
        }

        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.has("type") ? event.path("type").asText() : "";
            JsonNode dataObject = event.path("data").path("object");

            switch (eventType) {
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(dataObject);
                    break;
                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed(dataObject);
                    break;
                default:
                    log.info("Stripe webhook ignored event: {}", eventType);
                    break;
            }
        } catch (Exception e) {
            log.error("Stripe webhook processing error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error");
        }

        return ResponseEntity.ok("ok");
    }

    private void handlePaymentIntentSucceeded(JsonNode piNode) {
        String paymentIntentId = piNode.has("id") ? piNode.path("id").asText() : null;
        if (!StringUtils.hasText(paymentIntentId)) return;

        PaymentTransactionEntity txn = paymentTransactionMapperExt.selectByProviderTxnId(paymentIntentId);
        if (txn == null) {
            log.warn("Stripe webhook: no transaction found for pi {}", paymentIntentId);
            return;
        }

        Date now = new Date();
        txn.setStatus(PaymentTransactionStatus.SUCCEEDED.getCode());
        txn.setPaidAt(now);
        // Use the base mapper for update via selectByPrimaryKey pattern
        updateTransaction(txn);

        if (StringUtils.hasText(txn.getInvoiceId())) {
            InvoiceEntity invoice = invoiceMapper.selectByPrimaryKey(txn.getInvoiceId());
            if (invoice != null && !InvoiceStatus.PAID.getCode().equalsIgnoreCase(invoice.getStatus())) {
                invoice.setStatus(InvoiceStatus.PAID.getCode());
                invoiceMapper.updateByPrimaryKey(invoice);
                log.info("Invoice {} marked as PAID via Stripe webhook", invoice.getInvoiceId());
            }
        }
        log.info("PaymentIntent {} succeeded, txn {} updated", paymentIntentId, txn.getPaymentTransactionId());
    }

    private void handlePaymentIntentFailed(JsonNode piNode) {
        String paymentIntentId = piNode.has("id") ? piNode.path("id").asText() : null;
        if (!StringUtils.hasText(paymentIntentId)) return;

        PaymentTransactionEntity txn = paymentTransactionMapperExt.selectByProviderTxnId(paymentIntentId);
        if (txn == null) {
            log.warn("Stripe webhook: no transaction found for pi {}", paymentIntentId);
            return;
        }

        String failureMessage = piNode.has("last_payment_error")
                ? piNode.path("last_payment_error").path("message").asText("unknown")
                : "unknown";

        txn.setStatus(PaymentTransactionStatus.FAILED.getCode());
        txn.setFailureReason(failureMessage.length() > 255 ? failureMessage.substring(0, 255) : failureMessage);
        updateTransaction(txn);
        log.warn("PaymentIntent {} failed: {}", paymentIntentId, failureMessage);
    }

    private void updateTransaction(PaymentTransactionEntity txn) {
        try {
            // PaymentTransactionMapper.updateByPrimaryKey is the generated method
            // We access it via the ext mapper's session; alternatively inject base mapper.
            // For simplicity, use a direct JDBC-style approach via the mapper ext.
            // Since PaymentTransactionMapperExt doesn't have update, we use the base mapper
            // injected separately. But to avoid adding another dependency here,
            // we can add updateStatus to ext mapper.
            paymentTransactionMapperExt.updateStatusByProviderTxnId(
                    txn.getProviderTxnId(),
                    txn.getStatus(),
                    txn.getFailureReason(),
                    txn.getPaidAt()
            );
        } catch (Exception e) {
            log.error("Failed to update transaction {}: {}", txn.getPaymentTransactionId(), e.getMessage());
        }
    }

    private boolean verifySignature(String payload, String sigHeader, String secret) {
        try {
            String[] parts = sigHeader.split(",");
            String timestamp = null;
            String v1Sig = null;
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.startsWith("t=")) timestamp = trimmed.substring(2);
                if (trimmed.startsWith("v1=")) v1Sig = trimmed.substring(3);
            }
            if (timestamp == null || v1Sig == null) return false;

            String signedPayload = timestamp + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            String expected = bytesToHex(hash);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), v1Sig.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("Stripe signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
