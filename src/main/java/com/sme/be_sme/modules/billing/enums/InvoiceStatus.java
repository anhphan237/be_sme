package com.sme.be_sme.modules.billing.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Invoice lifecycle status.
 * When payment succeeds (e.g. via Stripe webhook), status becomes PAID.
 */
@Getter
@AllArgsConstructor
public enum InvoiceStatus {

    DRAFT("DRAFT", "Draft, not yet issued"),
    ISSUED("ISSUED", "Issued, awaiting payment"),
    PAID("PAID", "Payment successful"),
    VOID("VOID", "Voided / cancelled");

    private final String code;
    private final String description;

    public boolean isPaid() {
        return this == PAID;
    }

    public static InvoiceStatus fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        String normalized = code.trim().toUpperCase();
        for (InvoiceStatus s : values()) {
            if (s.code.equals(normalized)) return s;
        }
        return null;
    }
}
