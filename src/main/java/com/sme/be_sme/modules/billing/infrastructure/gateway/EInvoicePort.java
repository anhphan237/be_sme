package com.sme.be_sme.modules.billing.infrastructure.gateway;

import lombok.Builder;
import lombok.Getter;

/**
 * Port for e-invoice provider (e.g. local e-invoice API). Submit invoice data and get public URL.
 */
public interface EInvoicePort {

    /**
     * Submit invoice to e-invoice provider and return the public URL (or null if not supported / mock).
     *
     * @param companyId    tenant id
     * @param invoiceId    internal invoice id
     * @param invoiceNo   display invoice number
     * @param amountTotal total amount (e.g. VND)
     * @param currency    currency code
     * @return result with URL to download/view e-invoice
     */
    SubmitResult submit(String companyId, String invoiceId, String invoiceNo, int amountTotal, String currency);

    @Getter
    @Builder
    class SubmitResult {
        private final String eInvoiceUrl;
        private final boolean success;
    }
}
