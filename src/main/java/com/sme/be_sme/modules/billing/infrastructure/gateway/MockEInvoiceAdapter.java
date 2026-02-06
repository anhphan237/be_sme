package com.sme.be_sme.modules.billing.infrastructure.gateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock e-invoice adapter. Returns a placeholder URL; replace with real adapter when integrating with e-invoice API.
 */
@Component
@ConditionalOnProperty(name = "app.einvoice.provider", havingValue = "mock", matchIfMissing = true)
public class MockEInvoiceAdapter implements EInvoicePort {

    private static final String MOCK_BASE = "https://einvoice.example.com/mock";

    @Override
    public SubmitResult submit(String companyId, String invoiceId, String invoiceNo, int amountTotal, String currency) {
        String url = MOCK_BASE + "/" + invoiceId;
        return SubmitResult.builder()
                .eInvoiceUrl(url)
                .success(true)
                .build();
    }
}
