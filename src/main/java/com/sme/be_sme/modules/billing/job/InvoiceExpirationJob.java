package com.sme.be_sme.modules.billing.job;

import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapperExt;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionChangeRequestMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionChangeRequestEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceExpirationJob {

    private final InvoiceMapperExt invoiceMapperExt;
    private final SubscriptionChangeRequestMapper subscriptionChangeRequestMapper;

    @Scheduled(cron = "${app.billing.invoice-expiration.cron:0 */15 * * * ?}")
    @Transactional
    public void run() {
        Date now = new Date();
        List<InvoiceEntity> expiredInvoices = invoiceMapperExt.selectIssuedExpiredBefore(now);
        if (expiredInvoices == null || expiredInvoices.isEmpty()) {
            return;
        }
        log.info("InvoiceExpirationJob: expiring {} invoices", expiredInvoices.size());
        for (InvoiceEntity invoice : expiredInvoices) {
            int updated = invoiceMapperExt.markExpiredByInvoiceId(invoice.getInvoiceId(), now);
            if (updated != 1) {
                continue;
            }
            SubscriptionChangeRequestEntity pending = subscriptionChangeRequestMapper.selectPendingByInvoiceId(
                    invoice.getCompanyId(),
                    invoice.getInvoiceId()
            );
            if (pending == null) {
                continue;
            }
            subscriptionChangeRequestMapper.markFailed(
                    pending.getSubscriptionChangeRequestId(),
                    "invoice expired",
                    now
            );
        }
    }
}
