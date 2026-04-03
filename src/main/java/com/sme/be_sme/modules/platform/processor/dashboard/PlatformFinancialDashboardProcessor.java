package com.sme.be_sme.modules.platform.processor.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PaymentTransactionMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformFinancialDashboardRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformFinancialDashboardResponse;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformFinancialDashboardProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;
    private final InvoiceMapper invoiceMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformFinancialDashboardRequest request =
                objectMapper.convertValue(payload, PlatformFinancialDashboardRequest.class);

        if (!StringUtils.hasText(request.getStartDate()) || !StringUtils.hasText(request.getEndDate())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "startDate and endDate are required");
        }

        LocalDate startLocal = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate endLocal = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_LOCAL_DATE);

        if (endLocal.isBefore(startLocal)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "endDate must not be before startDate");
        }

        Date rangeStart = toDate(startLocal);
        Date rangeEnd = toDate(endLocal.plusDays(1)); // exclusive upper bound

        List<SubscriptionEntity> allSubs = subscriptionMapper.selectAll();
        Map<String, PlanEntity> planCache = planMapper.selectAll().stream()
                .collect(Collectors.toMap(PlanEntity::getPlanId, Function.identity(), (a, b) -> a));

        double mrr = calculateMrr(allSubs, planCache);
        double totalRevenue = calculateTotalRevenue(rangeStart, rangeEnd);
        int activeSubscriptions = countByStatus(allSubs, "ACTIVE");
        int newSubscriptions = countCreatedInRange(allSubs, rangeStart, rangeEnd);
        Double churnRate = calculateChurnRate(allSubs, rangeStart, rangeEnd);
        int failedPayments = countFailedPayments(rangeStart, rangeEnd);

        PlatformFinancialDashboardResponse response = new PlatformFinancialDashboardResponse();
        response.setMrr(mrr);
        response.setTotalRevenue(totalRevenue);
        response.setActiveSubscriptions(activeSubscriptions);
        response.setNewSubscriptions(newSubscriptions);
        response.setChurnRate(churnRate);
        response.setFailedPayments(failedPayments);
        return response;
    }

    private double calculateMrr(List<SubscriptionEntity> subs, Map<String, PlanEntity> planCache) {
        double mrr = 0.0;
        for (SubscriptionEntity sub : subs) {
            if (!"ACTIVE".equals(sub.getStatus())) {
                continue;
            }
            PlanEntity plan = planCache.get(sub.getPlanId());
            if (plan == null) {
                continue;
            }
            if ("YEARLY".equals(sub.getBillingCycle())) {
                mrr += (plan.getPriceVndYearly() != null ? plan.getPriceVndYearly() / 12.0 : 0);
            } else {
                mrr += (plan.getPriceVndMonthly() != null ? plan.getPriceVndMonthly() : 0);
            }
        }
        return mrr;
    }

    private double calculateTotalRevenue(Date rangeStart, Date rangeEnd) {
        double total = 0.0;
        for (InvoiceEntity inv : invoiceMapper.selectAll()) {
            if ("PAID".equals(inv.getStatus()) && inv.getCreatedAt() != null
                    && inRange(inv.getCreatedAt(), rangeStart, rangeEnd)) {
                total += (inv.getAmountTotal() != null ? inv.getAmountTotal() : 0);
            }
        }
        return total;
    }

    private int countByStatus(List<SubscriptionEntity> subs, String status) {
        int count = 0;
        for (SubscriptionEntity sub : subs) {
            if (status.equals(sub.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private int countCreatedInRange(List<SubscriptionEntity> subs, Date rangeStart, Date rangeEnd) {
        int count = 0;
        for (SubscriptionEntity sub : subs) {
            if (sub.getCreatedAt() != null && inRange(sub.getCreatedAt(), rangeStart, rangeEnd)) {
                count++;
            }
        }
        return count;
    }

    private Double calculateChurnRate(List<SubscriptionEntity> subs, Date rangeStart, Date rangeEnd) {
        int churnedCount = 0;
        int activeAtStart = 0;

        for (SubscriptionEntity sub : subs) {
            boolean isChurned = ("CANCELLED".equals(sub.getStatus()) || "SUSPENDED".equals(sub.getStatus()))
                    && sub.getUpdatedAt() != null
                    && inRange(sub.getUpdatedAt(), rangeStart, rangeEnd);

            if (isChurned) {
                churnedCount++;
            }

            boolean wasActiveAtStart = sub.getCreatedAt() != null
                    && sub.getCreatedAt().before(rangeStart)
                    && ("ACTIVE".equals(sub.getStatus()) || isChurned);

            if (wasActiveAtStart) {
                activeAtStart++;
            }
        }

        if (activeAtStart == 0) {
            return null;
        }
        return (double) churnedCount / activeAtStart;
    }

    private int countFailedPayments(Date rangeStart, Date rangeEnd) {
        int count = 0;
        for (PaymentTransactionEntity tx : paymentTransactionMapper.selectAll()) {
            if ("FAILED".equals(tx.getStatus()) && tx.getCreatedAt() != null
                    && inRange(tx.getCreatedAt(), rangeStart, rangeEnd)) {
                count++;
            }
        }
        return count;
    }

    private boolean inRange(Date date, Date rangeStart, Date rangeEnd) {
        return !date.before(rangeStart) && date.before(rangeEnd);
    }

    private Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
