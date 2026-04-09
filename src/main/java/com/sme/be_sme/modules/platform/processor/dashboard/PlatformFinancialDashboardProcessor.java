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
import com.sme.be_sme.modules.platform.processor.analytics.PlatformAnalyticsSupport;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.LocalDate;
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
        PlatformFinancialDashboardRequest request = objectMapper.convertValue(payload, PlatformFinancialDashboardRequest.class);

        if (!StringUtils.hasText(request.getStartDate()) || !StringUtils.hasText(request.getEndDate())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "startDate and endDate are required");
        }

        LocalDate startLocal = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate endLocal = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        if (endLocal.isBefore(startLocal)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "endDate must not be before startDate");
        }

        Date rangeStart = PlatformAnalyticsSupport.parseDate(request.getStartDate(), true);
        Date rangeEnd = PlatformAnalyticsSupport.parseDate(request.getEndDate(), false);

        List<SubscriptionEntity> allSubs = subscriptionMapper.selectAll();
        Map<String, PlanEntity> planCache = planMapper.selectAll().stream()
                .filter(p -> p != null && p.getPlanId() != null)
                .collect(Collectors.toMap(PlanEntity::getPlanId, Function.identity(), (a, b) -> a));

        PlatformFinancialDashboardResponse response = new PlatformFinancialDashboardResponse();
        response.setMrr(calculateMrr(allSubs, planCache));
        response.setTotalRevenue(calculateTotalRevenue(rangeStart, rangeEnd));
        response.setActiveSubscriptions(countByStatus(allSubs, "ACTIVE"));
        response.setNewSubscriptions(countCreatedInRange(allSubs, rangeStart, rangeEnd));
        response.setChurnRate(calculateChurnRate(allSubs, rangeStart, rangeEnd));
        response.setFailedPayments(countFailedPayments(rangeStart, rangeEnd));
        return response;
    }

    private double calculateMrr(List<SubscriptionEntity> subs, Map<String, PlanEntity> planCache) {
        double mrr = 0.0;
        for (SubscriptionEntity sub : subs) {
            if (sub == null || !"ACTIVE".equalsIgnoreCase(sub.getStatus())) {
                continue;
            }
            PlanEntity plan = planCache.get(sub.getPlanId());
            if (plan == null) {
                continue;
            }
            if ("YEARLY".equalsIgnoreCase(sub.getBillingCycle())) {
                mrr += plan.getPriceVndYearly() != null ? plan.getPriceVndYearly() / 12.0 : 0.0;
            } else {
                mrr += plan.getPriceVndMonthly() != null ? plan.getPriceVndMonthly() : 0.0;
            }
        }
        return mrr;
    }

    private double calculateTotalRevenue(Date rangeStart, Date rangeEnd) {
        double total = 0.0;
        for (InvoiceEntity inv : invoiceMapper.selectAll()) {
            if (inv != null && "PAID".equalsIgnoreCase(inv.getStatus())
                    && PlatformAnalyticsSupport.inRange(inv.getCreatedAt(), rangeStart, rangeEnd)) {
                total += inv.getAmountTotal() != null ? inv.getAmountTotal() : 0.0;
            }
        }
        return total;
    }

    private int countByStatus(List<SubscriptionEntity> subs, String status) {
        int count = 0;
        for (SubscriptionEntity sub : subs) {
            if (sub != null && status.equalsIgnoreCase(sub.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private int countCreatedInRange(List<SubscriptionEntity> subs, Date rangeStart, Date rangeEnd) {
        int count = 0;
        for (SubscriptionEntity sub : subs) {
            if (sub != null && PlatformAnalyticsSupport.inRange(sub.getCreatedAt(), rangeStart, rangeEnd)) {
                count++;
            }
        }
        return count;
    }

    private Double calculateChurnRate(List<SubscriptionEntity> subs, Date rangeStart, Date rangeEnd) {
        int churnedCount = 0;
        int activeAtStart = 0;
        for (SubscriptionEntity sub : subs) {
            if (sub == null) {
                continue;
            }
            boolean isChurned = ("CANCELLED".equalsIgnoreCase(sub.getStatus()) || "SUSPENDED".equalsIgnoreCase(sub.getStatus()))
                    && PlatformAnalyticsSupport.inRange(sub.getUpdatedAt(), rangeStart, rangeEnd);
            if (isChurned) {
                churnedCount++;
            }
            boolean wasActiveAtStart = sub.getCreatedAt() != null && sub.getCreatedAt().before(rangeStart)
                    && ("ACTIVE".equalsIgnoreCase(sub.getStatus()) || isChurned);
            if (wasActiveAtStart) {
                activeAtStart++;
            }
        }
        return activeAtStart > 0 ? (double) churnedCount / activeAtStart : 0.0;
    }

    private int countFailedPayments(Date rangeStart, Date rangeEnd) {
        int count = 0;
        for (PaymentTransactionEntity tx : paymentTransactionMapper.selectAll()) {
            if (tx != null && "FAILED".equalsIgnoreCase(tx.getStatus())
                    && PlatformAnalyticsSupport.inRange(tx.getCreatedAt(), rangeStart, rangeEnd)) {
                count++;
            }
        }
        return count;
    }
}
