package com.sme.be_sme.modules.billing.service;

import com.sme.be_sme.modules.billing.enums.InvoiceStatus;
import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.UsageMonthlyMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.UsageMonthlyEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Core logic to generate an invoice for a subscription and period.
 * Used by InvoiceGenerateProcessor (gateway) and RecurringInvoiceJob.
 */
@Service
@RequiredArgsConstructor
public class InvoiceGenerateCoreService {

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int OVERAGE_UNIT_PRICE_VND = 10000;

    private final InvoiceMapper invoiceMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;
    private final UsageMonthlyMapper usageMonthlyMapper;

    /**
     * Generate invoice for the given subscription and period. Caller is responsible for e-invoice submit and update.
     *
     * @param companyId      tenant (must match subscription)
     * @param subscriptionId subscription to bill
     * @param periodStart    period start (inclusive)
     * @param periodEnd      period end (inclusive)
     * @return the created invoice entity
     */
    public InvoiceEntity generateForPeriod(String companyId, String subscriptionId, LocalDate periodStart, LocalDate periodEnd) {
        if (!StringUtils.hasText(companyId) || !StringUtils.hasText(subscriptionId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "companyId and subscriptionId are required");
        }
        if (periodEnd.isBefore(periodStart)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "periodEnd must be >= periodStart");
        }

        SubscriptionEntity subscription = subscriptionMapper.selectByPrimaryKey(subscriptionId.trim());
        if (subscription == null || !companyId.trim().equals(subscription.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "subscription not found");
        }

        PlanEntity plan = planMapper.selectByPrimaryKey(subscription.getPlanId());
        if (plan == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "plan not found");
        }

        int amountTotal = calculateAmount(subscription, plan, periodStart);
        Date now = new Date();

        InvoiceEntity invoice = new InvoiceEntity();
        String invoiceId = UuidGenerator.generate();
        invoice.setInvoiceId(invoiceId);
        invoice.setCompanyId(companyId.trim());
        invoice.setSubscriptionId(subscription.getSubscriptionId());
        invoice.setInvoiceNo(buildInvoiceNo(invoiceId));
        invoice.setAmountTotal(amountTotal);
        invoice.setCurrency("VND");
        invoice.setStatus(InvoiceStatus.ISSUED.getCode());
        invoice.setIssuedAt(now);
        invoice.setDueAt(addDays(now, 7));
        invoice.setCreatedAt(now);

        int inserted = invoiceMapper.insert(invoice);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "generate invoice failed");
        }
        return invoice;
    }

    private int calculateAmount(SubscriptionEntity subscription, PlanEntity plan, LocalDate periodStart) {
        String billingCycle = subscription.getBillingCycle() == null ? "MONTHLY" : subscription.getBillingCycle();
        int basePrice = "YEARLY".equalsIgnoreCase(billingCycle)
                ? safeAmount(plan.getPriceVndYearly())
                : safeAmount(plan.getPriceVndMonthly());

        String monthKey = periodStart.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        UsageMonthlyEntity usage = findUsage(subscription.getCompanyId(), subscription.getSubscriptionId(), monthKey);
        int usageCount = usage == null || usage.getOnboardedEmployeeCount() == null
                ? 0
                : usage.getOnboardedEmployeeCount();

        int limit = plan.getEmployeeLimitPerMonth() == null ? 0 : plan.getEmployeeLimitPerMonth();
        int overage = Math.max(0, usageCount - limit);
        return basePrice + (overage * OVERAGE_UNIT_PRICE_VND);
    }

    private UsageMonthlyEntity findUsage(String companyId, String subscriptionId, String month) {
        List<UsageMonthlyEntity> all = usageMonthlyMapper.selectAll();
        if (all == null) return null;
        return all.stream()
                .filter(Objects::nonNull)
                .filter(u -> companyId.equals(u.getCompanyId()))
                .filter(u -> subscriptionId.equals(u.getSubscriptionId()))
                .filter(u -> month.equals(u.getMonth()))
                .findFirst()
                .orElse(null);
    }

    private static int safeAmount(Integer amount) {
        return amount == null ? 0 : amount;
    }

    private static String buildInvoiceNo(String invoiceId) {
        String suffix = invoiceId.length() > 6 ? invoiceId.substring(invoiceId.length() - 6) : invoiceId;
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "INV-" + today + "-" + suffix;
    }

    private static Date addDays(Date start, int days) {
        LocalDate date = start.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Date.from(date.plusDays(days).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
