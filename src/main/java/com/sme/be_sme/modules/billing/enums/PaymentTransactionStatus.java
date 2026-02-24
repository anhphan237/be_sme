package com.sme.be_sme.modules.billing.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Payment transaction status.
 * INIT/PENDING when created; SUCCEEDED when payment completes; FAILED on failure; REFUNDED when refunded.
 */
@Getter
@AllArgsConstructor
public enum PaymentTransactionStatus {

    INIT("INIT", "Initial / created"),
    PENDING("PENDING", "Awaiting payment (e.g. requires_payment_method, requires_confirmation)"),
    SUCCEEDED("SUCCEEDED", "Payment successful"),
    FAILED("FAILED", "Payment failed"),
    REFUNDED("REFUNDED", "Refunded");

    private final String code;
    private final String description;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == REFUNDED;
    }

    public boolean isSuccess() {
        return this == SUCCEEDED;
    }

    public static PaymentTransactionStatus fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        String normalized = code.trim().toUpperCase();
        for (PaymentTransactionStatus s : values()) {
            if (s.code.equals(normalized)) return s;
        }
        // Map Stripe lowercase statuses
        if ("succeeded".equalsIgnoreCase(code)) return SUCCEEDED;
        if ("failed".equalsIgnoreCase(code)) return FAILED;
        return PENDING; // requires_payment_method, requires_confirmation, etc.
    }
}
