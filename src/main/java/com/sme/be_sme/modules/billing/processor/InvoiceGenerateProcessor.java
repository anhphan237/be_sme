package com.sme.be_sme.modules.billing.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.api.request.InvoiceGenerateRequest;
import com.sme.be_sme.modules.billing.api.response.InvoiceGenerateResponse;
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
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class InvoiceGenerateProcessor extends BaseBizProcessor<BizContext> {

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int OVERAGE_UNIT_PRICE_VND = 10000;

    private final ObjectMapper objectMapper;
    private final InvoiceMapper invoiceMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;
    private final UsageMonthlyMapper usageMonthlyMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        InvoiceGenerateRequest request = objectMapper.convertValue(payload, InvoiceGenerateRequest.class);
        validate(context, request);

        SubscriptionEntity subscription = subscriptionMapper.selectByPrimaryKey(request.getSubscriptionId().trim());
        if (subscription == null || !context.getTenantId().trim().equals(subscription.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "subscription not found");
        }

        PlanEntity plan = planMapper.selectByPrimaryKey(subscription.getPlanId());
        if (plan == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "plan not found");
        }

        LocalDate periodStart = parseDate(request.getPeriodStart(), "periodStart");
        LocalDate periodEnd = parseDate(request.getPeriodEnd(), "periodEnd");
        if (periodEnd.isBefore(periodStart)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "periodEnd must be >= periodStart");
        }

        int amountTotal = calculateAmount(subscription, plan, periodStart);
        Date now = new Date();

        InvoiceEntity invoice = new InvoiceEntity();
        String invoiceId = UuidGenerator.generate();
        invoice.setInvoiceId(invoiceId);
        invoice.setCompanyId(context.getTenantId().trim());
        invoice.setSubscriptionId(subscription.getSubscriptionId());
        invoice.setInvoiceNo(buildInvoiceNo(invoiceId));
        invoice.setAmountTotal(amountTotal);
        invoice.setCurrency("VND");
        invoice.setStatus("ISSUED");
        invoice.setIssuedAt(now);
        invoice.setDueAt(addDays(now, 7));
        invoice.setCreatedAt(now);

        int inserted = invoiceMapper.insert(invoice);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "generate invoice failed");
        }

        InvoiceGenerateResponse response = new InvoiceGenerateResponse();
        response.setInvoiceId(invoice.getInvoiceId());
        response.setStatus(invoice.getStatus());
        return response;
    }

    private static void validate(BizContext context, InvoiceGenerateRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getSubscriptionId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "subscriptionId is required");
        }
        if (!StringUtils.hasText(request.getPeriodStart())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "periodStart is required");
        }
        if (!StringUtils.hasText(request.getPeriodEnd())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "periodEnd is required");
        }
    }

    private static LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value.trim(), PERIOD_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, fieldName + " must be ISO-8601 yyyy-MM-dd");
        }
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
        return usageMonthlyMapper.selectAll().stream()
                .filter(usage -> usage != null)
                .filter(usage -> companyId.equals(usage.getCompanyId()))
                .filter(usage -> subscriptionId.equals(usage.getSubscriptionId()))
                .filter(usage -> month.equals(usage.getMonth()))
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
