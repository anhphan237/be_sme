package com.sme.be_sme.modules.analytics.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.analytics.api.request.PlatformSubscriptionMetricsRequest;
import com.sme.be_sme.modules.analytics.api.response.PlatformSubscriptionMetricsResponse;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformSubscriptionMetricsProcessor extends BaseBizProcessor<BizContext> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ObjectMapper objectMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformSubscriptionMetricsRequest request = objectMapper.convertValue(payload, PlatformSubscriptionMetricsRequest.class);
        validate(context, request);

        LocalDate startDate = parseDate(request.getStartDate(), "startDate");
        LocalDate endDate = parseDate(request.getEndDate(), "endDate");
        if (endDate.isBefore(startDate)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "endDate must be >= startDate");
        }

        Date rangeStart = atStartOfDay(startDate);
        Date rangeEnd = atEndOfDay(endDate);
        Map<String, PlanEntity> plansById = planMapper.selectAll().stream()
                .filter(Objects::nonNull)
                .filter(plan -> StringUtils.hasText(plan.getPlanId()))
                .collect(Collectors.toMap(PlanEntity::getPlanId, plan -> plan, (left, right) -> left));

        List<SubscriptionEntity> subscriptions = subscriptionMapper.selectAll();
        int activeSubscriptions = 0;
        double monthlyRecurringRevenue = 0.0;
        int activeAtStart = 0;
        int churnedCount = 0;

        for (SubscriptionEntity subscription : subscriptions) {
            if (subscription == null) {
                continue;
            }
            if (isActive(subscription)) {
                if (isWithinRange(subscription, rangeStart, rangeEnd)) {
                    activeSubscriptions++;
                    monthlyRecurringRevenue += resolveMonthlyPrice(subscription, plansById);
                }
                if (wasActiveAtPoint(subscription, rangeStart)) {
                    activeAtStart++;
                }
            } else if (isChurnedStatus(subscription.getStatus()) && isUpdatedInRange(subscription.getUpdatedAt(), rangeStart, rangeEnd)) {
                churnedCount++;
            }
        }

        Double churnRateValue = activeAtStart > 0 ? (double) churnedCount / activeAtStart : null;

        PlatformSubscriptionMetricsResponse response = new PlatformSubscriptionMetricsResponse();
        response.setActiveSubscriptions(activeSubscriptions);
        response.setMonthlyRecurringRevenue(monthlyRecurringRevenue);
        response.setActiveAtStart(activeAtStart);
        response.setChurnedCount(churnedCount);
        response.setChurnRate(churnRateValue);
        return response;
    }

    private static void validate(BizContext context, PlatformSubscriptionMetricsRequest request) {
        if (context == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "context is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getStartDate())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "startDate is required");
        }
        if (!StringUtils.hasText(request.getEndDate())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "endDate is required");
        }
    }

    private static LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, fieldName + " must be ISO-8601 yyyy-MM-dd");
        }
    }

    private static Date atStartOfDay(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static Date atEndOfDay(LocalDate date) {
        return Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static boolean isActive(SubscriptionEntity subscription) {
        return subscription.getStatus() != null && "ACTIVE".equalsIgnoreCase(subscription.getStatus());
    }

    private static boolean isWithinRange(SubscriptionEntity subscription, Date start, Date endExclusive) {
        Date periodStart = subscription.getCurrentPeriodStart();
        Date periodEnd = subscription.getCurrentPeriodEnd();
        if (periodStart == null && periodEnd == null) {
            return false;
        }
        if (periodStart != null && periodStart.after(endExclusive)) {
            return false;
        }
        if (periodEnd != null && !periodEnd.after(start)) {
            return false;
        }
        return true;
    }

    private static double resolveMonthlyPrice(SubscriptionEntity subscription, Map<String, PlanEntity> plansById) {
        PlanEntity plan = plansById.get(subscription.getPlanId());
        if (plan == null) {
            return 0.0;
        }
        Integer monthly = plan.getPriceVndMonthly();
        Integer yearly = plan.getPriceVndYearly();
        String cycle = subscription.getBillingCycle();

        if (cycle != null && "YEARLY".equalsIgnoreCase(cycle)) {
            if (yearly == null) {
                return 0.0;
            }
            return yearly / 12.0;
        }
        if (monthly != null) {
            return monthly;
        }
        return yearly == null ? 0.0 : yearly / 12.0;
    }

    /** Subscription was active at the given point in time (period contains that date). */
    private static boolean wasActiveAtPoint(SubscriptionEntity subscription, Date pointInTime) {
        Date periodStart = subscription.getCurrentPeriodStart();
        Date periodEnd = subscription.getCurrentPeriodEnd();
        if (periodStart != null && periodStart.after(pointInTime)) {
            return false;
        }
        if (periodEnd != null && !periodEnd.after(pointInTime)) {
            return false;
        }
        return true;
    }

    private static boolean isChurnedStatus(String status) {
        if (status == null) return false;
        String s = status.trim().toUpperCase();
        return "CANCELLED".equals(s) || "SUSPENDED".equals(s);
    }

    /** updated_at in [start, end) (end exclusive). */
    private static boolean isUpdatedInRange(Date updatedAt, Date start, Date endExclusive) {
        if (updatedAt == null) return false;
        return !updatedAt.before(start) && updatedAt.before(endExclusive);
    }
}
