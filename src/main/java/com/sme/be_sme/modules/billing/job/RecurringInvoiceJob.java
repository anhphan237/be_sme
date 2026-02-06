package com.sme.be_sme.modules.billing.job;

import com.sme.be_sme.modules.billing.infrastructure.gateway.EInvoicePort;
import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapperExt;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.billing.service.InvoiceGenerateCoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Generates monthly invoices for all active subscriptions that do not yet have an invoice for the current month.
 * Runs at start of month (configurable). After generating, submits to e-invoice and updates invoice URL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecurringInvoiceJob {

    private final SubscriptionMapper subscriptionMapper;
    private final InvoiceMapperExt invoiceMapperExt;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceGenerateCoreService invoiceGenerateCoreService;
    private final EInvoicePort eInvoicePort;

    @Scheduled(cron = "${app.billing.recurring-invoice.cron:0 0 1 1 * *}") // default: 1st day of month at 00:00
    public void run() {
        LocalDate today = LocalDate.now();
        LocalDate periodStart = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate periodEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        Date issuedFrom = Date.from(periodStart.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date issuedTo = Date.from(periodEnd.atTime(23, 59, 59, 999_999_999).atZone(ZoneId.systemDefault()).toInstant());

        List<SubscriptionEntity> active = getActiveSubscriptions();
        if (active == null || active.isEmpty()) {
            return;
        }
        log.info("RecurringInvoiceJob: processing {} active subscriptions for period {}-{}",
                active.size(), periodStart, periodEnd);

        for (SubscriptionEntity sub : active) {
            try {
                processSubscription(sub, periodStart, periodEnd, issuedFrom, issuedTo);
            } catch (Exception e) {
                log.warn("RecurringInvoiceJob: failed for subscription {}: {}", sub.getSubscriptionId(), e.getMessage());
            }
        }
    }

    private List<SubscriptionEntity> getActiveSubscriptions() {
        List<SubscriptionEntity> all = subscriptionMapper.selectAll();
        if (all == null) return List.of();
        return all.stream()
                .filter(Objects::nonNull)
                .filter(s -> "ACTIVE".equalsIgnoreCase(trimLower(s.getStatus())))
                .collect(Collectors.toList());
    }

    private void processSubscription(SubscriptionEntity sub, LocalDate periodStart, LocalDate periodEnd,
                                    Date issuedFrom, Date issuedTo) {
        String companyId = sub.getCompanyId();
        String subscriptionId = sub.getSubscriptionId();
        InvoiceEntity existing = invoiceMapperExt.selectBySubscriptionIdAndIssuedBetween(
                companyId, subscriptionId, issuedFrom, issuedTo);
        if (existing != null) {
            log.debug("RecurringInvoiceJob: subscription {} already has invoice for this month", subscriptionId);
            return;
        }

        InvoiceEntity invoice = invoiceGenerateCoreService.generateForPeriod(
                companyId, subscriptionId, periodStart, periodEnd);

        if (eInvoicePort != null) {
            EInvoicePort.SubmitResult result = eInvoicePort.submit(
                    invoice.getCompanyId(),
                    invoice.getInvoiceId(),
                    invoice.getInvoiceNo(),
                    invoice.getAmountTotal(),
                    invoice.getCurrency()
            );
            if (result.isSuccess() && result.getEInvoiceUrl() != null) {
                invoice.seteInvoiceUrl(result.getEInvoiceUrl());
                invoiceMapper.updateByPrimaryKey(invoice);
            }
        }
        log.info("RecurringInvoiceJob: generated invoice {} for subscription {}", invoice.getInvoiceId(), subscriptionId);
    }

    private static String trimLower(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
