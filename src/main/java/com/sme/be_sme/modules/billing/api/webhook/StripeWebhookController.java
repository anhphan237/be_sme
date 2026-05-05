package com.sme.be_sme.modules.billing.api.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.enums.InvoiceStatus;
import com.sme.be_sme.modules.billing.enums.PaymentTransactionStatus;
import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapperExt;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PaymentTransactionMapperExt;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionChangeRequestMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapperExt;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionPlanHistoryMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionChangeRequestEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionPlanHistoryEntity;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserRoleMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private static final String TEMPLATE_PAYMENT_SUCCEEDED = "PAYMENT_SUCCEEDED";

    private final ObjectMapper objectMapper;
    private final PaymentTransactionMapperExt paymentTransactionMapperExt;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceMapperExt invoiceMapperExt;
    private final SubscriptionChangeRequestMapper subscriptionChangeRequestMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionMapperExt subscriptionMapperExt;
    private final SubscriptionPlanHistoryMapper subscriptionPlanHistoryMapper;
    private final NotificationService notificationService;
    private final UserRoleMapperExt userRoleMapperExt;
    private final UserMapperExt userMapperExt;
    private final CompanyMapper companyMapper;

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

        boolean alreadySucceeded = PaymentTransactionStatus.SUCCEEDED.getCode().equalsIgnoreCase(txn.getStatus());

        Date now = new Date();
        txn.setStatus(PaymentTransactionStatus.SUCCEEDED.getCode());
        txn.setPaidAt(now);
        // Use the base mapper for update via selectByPrimaryKey pattern
        updateTransaction(txn);

        InvoiceEntity invoiceForNotify = null;
        if (StringUtils.hasText(txn.getInvoiceId())) {
            InvoiceEntity invoice = invoiceMapper.selectByPrimaryKey(txn.getInvoiceId());
            if (invoice != null) {
                invoiceForNotify = invoice;
                if (!InvoiceStatus.PAID.getCode().equalsIgnoreCase(invoice.getStatus())) {
                    invoice.setStatus(InvoiceStatus.PAID.getCode());
                    invoiceMapper.updateByPrimaryKey(invoice);
                    log.info("Invoice {} marked as PAID via Stripe webhook", invoice.getInvoiceId());
                }
            }
        }
        applyPendingPlanChangeIfAny(txn, now);

        if (!alreadySucceeded) {
            try {
                notifyHrPaymentSucceeded(txn, invoiceForNotify);
            } catch (Exception e) {
                log.warn("Stripe webhook: HR notification failed: {}", e.getMessage(), e);
            }
        }

        log.info("PaymentIntent {} succeeded, txn {} updated", paymentIntentId, txn.getPaymentTransactionId());
    }

    private void notifyHrPaymentSucceeded(PaymentTransactionEntity txn, InvoiceEntity invoice) {
        if (txn == null || !StringUtils.hasText(txn.getCompanyId())) {
            return;
        }
        String companyId = txn.getCompanyId().trim();
        Set<String> hrUserIds = new LinkedHashSet<>();
        addUserIdsForRole(companyId, "HR", hrUserIds);
        addUserIdsForRole(companyId, "HR_ADMIN", hrUserIds);
        if (hrUserIds.isEmpty()) {
            return;
        }

        int amount = txn.getAmount() != null ? txn.getAmount() : 0;
        String currency = StringUtils.hasText(txn.getCurrency()) ? txn.getCurrency().trim() : "VND";
        String invoiceLabel = invoice != null && StringUtils.hasText(invoice.getInvoiceNo())
                ? invoice.getInvoiceNo().trim()
                : (StringUtils.hasText(txn.getInvoiceId()) ? txn.getInvoiceId().trim() : txn.getPaymentTransactionId());
        String title = "Payment received";
        String content = invoice != null && StringUtils.hasText(invoice.getInvoiceNo())
                ? String.format("Payment for invoice %s (%d %s) completed successfully.", invoiceLabel, amount, currency)
                : String.format("A payment of %d %s completed successfully.", amount, currency);

        String companyName = "";
        CompanyEntity company = companyMapper.selectByPrimaryKey(companyId);
        if (company != null && StringUtils.hasText(company.getName())) {
            companyName = company.getName().trim();
        }

        Map<String, String> emailPlaceholders = new HashMap<>();
        emailPlaceholders.put("invoiceNo", invoiceLabel);
        emailPlaceholders.put("amountTotal", String.valueOf(amount));
        emailPlaceholders.put("currency", currency);
        emailPlaceholders.put("companyName", companyName.isEmpty() ? companyId : companyName);

        String refId = StringUtils.hasText(txn.getInvoiceId()) ? txn.getInvoiceId().trim() : txn.getPaymentTransactionId();
        for (String userId : hrUserIds) {
            UserEntity user = userMapperExt.selectByCompanyIdAndUserId(companyId, userId);
            if (user == null) {
                continue;
            }
            NotificationCreateParams params = NotificationCreateParams.builder()
                    .companyId(companyId)
                    .userId(userId)
                    .type("PAYMENT_SUCCEEDED")
                    .title(title)
                    .content(content)
                    .refType(StringUtils.hasText(txn.getInvoiceId()) ? "INVOICE" : "PAYMENT")
                    .refId(refId)
                    .sendEmail(true)
                    .emailTemplate(TEMPLATE_PAYMENT_SUCCEEDED)
                    .emailPlaceholders(emailPlaceholders)
                    .toEmail(user.getEmail())
                    .build();
            notificationService.create(params);
        }
    }

    private void addUserIdsForRole(String companyId, String roleCode, Set<String> out) {
        List<String> ids = userRoleMapperExt.selectUserIdsByCompanyAndRoleCode(companyId, roleCode);
        if (ids == null) {
            return;
        }
        for (String id : ids) {
            if (StringUtils.hasText(id)) {
                out.add(id.trim());
            }
        }
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
        markInvoiceFailedIfPayable(txn);
        markPendingPlanChangeFailed(txn, failureMessage);
        log.warn("PaymentIntent {} failed: {}", paymentIntentId, failureMessage);
    }

    private void markInvoiceFailedIfPayable(PaymentTransactionEntity txn) {
        if (txn == null || !StringUtils.hasText(txn.getInvoiceId())) {
            return;
        }
        invoiceMapperExt.updateStatusIfCurrentIn(
                txn.getInvoiceId().trim(),
                InvoiceStatus.FAILED.getCode(),
                InvoiceStatus.ISSUED.getCode(),
                InvoiceStatus.FAILED.getCode()
        );
    }

    @Transactional
    protected void applyPendingPlanChangeIfAny(PaymentTransactionEntity txn, Date now) {
        if (!StringUtils.hasText(txn.getInvoiceId()) || !StringUtils.hasText(txn.getCompanyId())) {
            return;
        }
        SubscriptionChangeRequestEntity pending = subscriptionChangeRequestMapper.selectPendingByInvoiceId(
                txn.getCompanyId(), txn.getInvoiceId()
        );
        if (pending == null) {
            return;
        }

        try {
            SubscriptionEntity subscription = subscriptionMapper.selectByPrimaryKey(pending.getSubscriptionId());
            if (subscription == null || !txn.getCompanyId().equals(subscription.getCompanyId())) {
                subscriptionChangeRequestMapper.markFailed(
                        pending.getSubscriptionChangeRequestId(),
                        "subscription not found while applying pending plan change",
                        now
                );
                log.warn("Stripe webhook: pending change {} cannot be applied, subscription missing", pending.getSubscriptionChangeRequestId());
                return;
            }

            int updated = subscriptionMapperExt.updatePlanAndStatus(
                    subscription.getSubscriptionId(),
                    pending.getNewPlanId(),
                    pending.getBillingCycle(),
                    "ACTIVE",
                    now
            );
            if (updated == 0) {
                SubscriptionEntity latest = subscriptionMapper.selectByPrimaryKey(subscription.getSubscriptionId());
                if (latest == null) {
                    subscriptionChangeRequestMapper.markFailed(
                            pending.getSubscriptionChangeRequestId(),
                            "subscription missing after update attempt",
                            now
                    );
                    return;
                }
                boolean alreadyApplied = equalsTrimmed(latest.getPlanId(), pending.getNewPlanId())
                        && equalsTrimmed(latest.getBillingCycle(), pending.getBillingCycle());
                if (!alreadyApplied) {
                    subscriptionChangeRequestMapper.markFailed(
                            pending.getSubscriptionChangeRequestId(),
                            "could not update subscription to requested plan",
                            now
                    );
                    return;
                }
            }

            try {
                savePlanChangeHistory(subscription, pending.getOldPlanId(), pending.getNewPlanId(), pending.getBillingCycle(), pending.getRequestedBy(), now);
            } catch (Exception historyEx) {
                log.warn("Stripe webhook: history write failed for pending change {} - {}",
                        pending.getSubscriptionChangeRequestId(), historyEx.getMessage(), historyEx);
            }

            subscriptionChangeRequestMapper.markApplied(pending.getSubscriptionChangeRequestId(), now, now);
            log.info("Stripe webhook: applied pending subscription change {} for subscription {}",
                    pending.getSubscriptionChangeRequestId(), pending.getSubscriptionId());
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "apply pending plan change failed" : e.getMessage();
            subscriptionChangeRequestMapper.markFailed(
                    pending.getSubscriptionChangeRequestId(),
                    msg.length() > 255 ? msg.substring(0, 255) : msg,
                    now
            );
            log.error("Stripe webhook: failed applying pending subscription change {} - {}",
                    pending.getSubscriptionChangeRequestId(), e.getMessage(), e);
        }
    }

    private void markPendingPlanChangeFailed(PaymentTransactionEntity txn, String failureMessage) {
        if (!StringUtils.hasText(txn.getInvoiceId()) || !StringUtils.hasText(txn.getCompanyId())) {
            return;
        }
        SubscriptionChangeRequestEntity pending = subscriptionChangeRequestMapper.selectPendingByInvoiceId(
                txn.getCompanyId(), txn.getInvoiceId()
        );
        if (pending == null) return;
        Date now = new Date();
        String reason = failureMessage == null ? "payment failed" : failureMessage;
        if (reason.length() > 255) {
            reason = reason.substring(0, 255);
        }
        subscriptionChangeRequestMapper.markFailed(pending.getSubscriptionChangeRequestId(), reason, now);
        log.info("Stripe webhook: pending subscription change {} marked FAILED", pending.getSubscriptionChangeRequestId());
    }

    private void updateTransaction(PaymentTransactionEntity txn) {
        int updated = paymentTransactionMapperExt.updateStatusByProviderTxnId(
                txn.getProviderTxnId(),
                txn.getStatus(),
                txn.getFailureReason(),
                txn.getPaidAt()
        );
        if (updated <= 0) {
            throw new IllegalStateException("Failed to update transaction " + txn.getPaymentTransactionId());
        }
    }

    private void savePlanChangeHistory(SubscriptionEntity subscription,
                                       String oldPlanId,
                                       String newPlanId,
                                       String billingCycle,
                                       String changedBy,
                                       Date changeTime) {
        subscriptionPlanHistoryMapper.closeOpenBySubscription(subscription.getCompanyId(), subscription.getSubscriptionId(), changeTime);
        SubscriptionPlanHistoryEntity history = new SubscriptionPlanHistoryEntity();
        history.setSubscriptionPlanHistoryId(UuidGenerator.generate());
        history.setCompanyId(subscription.getCompanyId());
        history.setSubscriptionId(subscription.getSubscriptionId());
        history.setOldPlanId(oldPlanId);
        history.setNewPlanId(newPlanId);
        history.setBillingCycle(billingCycle);
        history.setChangedBy(changedBy);
        history.setChangedAt(changeTime);
        history.setEffectiveFrom(changeTime);
        history.setEffectiveTo(null);
        history.setCreatedAt(changeTime);
        subscriptionPlanHistoryMapper.insert(history);
    }

    private static boolean equalsTrimmed(String left, String right) {
        String l = left == null ? null : left.trim();
        String r = right == null ? null : right.trim();
        if (l == null) return r == null;
        return l.equals(r);
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
