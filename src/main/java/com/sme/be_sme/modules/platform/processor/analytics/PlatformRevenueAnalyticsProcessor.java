package com.sme.be_sme.modules.platform.processor.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformRevenueAnalyticsRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformRevenueAnalyticsResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformRevenueAnalyticsProcessor extends BaseBizProcessor<BizContext> {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PAID = "PAID";
    private static final String BILLING_YEARLY = "YEARLY";

    private final ObjectMapper objectMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;
    private final InvoiceMapper invoiceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformRevenueAnalyticsRequest request =
                objectMapper.convertValue(payload, PlatformRevenueAnalyticsRequest.class);

        Date startDate = parseDate(request.getStartDate(), true);
        Date endDate = parseDate(request.getEndDate(), false);

        List<SubscriptionEntity> allSubs = subscriptionMapper.selectAll();
        List<PlanEntity> allPlans = planMapper.selectAll();
        List<InvoiceEntity> allInvoices = invoiceMapper.selectAll();

        Map<String, PlanEntity> planCache = new LinkedHashMap<>();
        for (PlanEntity plan : allPlans) {
            if (plan != null && plan.getPlanId() != null) {
                planCache.put(plan.getPlanId(), plan);
            }
        }

        double mrr = 0.0;
        double totalRevenue = 0.0;

        Map<String, RevenueAccumulator> byPlan = new LinkedHashMap<>();

        for (SubscriptionEntity sub : allSubs) {
            if (sub == null || !STATUS_ACTIVE.equalsIgnoreCase(sub.getStatus())) {
                continue;
            }

            PlanEntity plan = planCache.get(sub.getPlanId());
            if (plan == null) {
                continue;
            }

            double monthlyRevenue = toMonthlyRevenue(sub, plan);
            mrr += monthlyRevenue;

            RevenueAccumulator acc = byPlan.computeIfAbsent(
                    safe(sub.getPlanId()),
                    k -> new RevenueAccumulator(safe(sub.getPlanId()))
            );
            acc.subscriptionCount++;
            acc.revenue += monthlyRevenue;
            acc.planName = readString(plan, "getName");
            acc.planCode = readString(plan, "getCode");
        }

        for (InvoiceEntity inv : allInvoices) {
            if (inv == null) {
                continue;
            }
            if (STATUS_PAID.equalsIgnoreCase(inv.getStatus()) && inRange(inv.getCreatedAt(), startDate, endDate)) {
                totalRevenue += inv.getAmountTotal() != null ? inv.getAmountTotal() : 0.0;
            }
        }

        List<PlatformRevenueAnalyticsResponse.RevenueByPlanItem> revenueByPlans = new ArrayList<>();
        for (RevenueAccumulator acc : byPlan.values()) {
            PlatformRevenueAnalyticsResponse.RevenueByPlanItem item =
                    new PlatformRevenueAnalyticsResponse.RevenueByPlanItem();

            invokeSetterIfExists(item, "setPlanId", String.class, acc.planId);
            invokeSetterIfExists(item, "setPlanCode", String.class, acc.planCode);
            invokeSetterIfExists(item, "setPlanName", String.class, acc.planName);
            invokeSetterIfExists(item, "setRevenue", Double.class, acc.revenue);
            invokeSetterIfExists(item, "setSubscriptionCount", Integer.class, acc.subscriptionCount);

            revenueByPlans.add(item);
        }

        PlatformRevenueAnalyticsResponse response = new PlatformRevenueAnalyticsResponse();
        response.setMrr(mrr);
        response.setArr(mrr * 12);
        response.setTotalRevenue(totalRevenue);
        response.setRevenueByPlans(revenueByPlans);
        return response;
    }

    private double toMonthlyRevenue(SubscriptionEntity sub, PlanEntity plan) {
        if (BILLING_YEARLY.equalsIgnoreCase(sub.getBillingCycle())) {
            return plan.getPriceVndYearly() != null ? plan.getPriceVndYearly() / 12.0 : 0.0;
        }
        return plan.getPriceVndMonthly() != null ? plan.getPriceVndMonthly() : 0.0;
    }

    private boolean inRange(Date value, Date start, Date end) {
        if (value == null) {
            return false;
        }
        if (start != null && value.before(start)) {
            return false;
        }
        if (end != null && !value.before(end)) {
            return false;
        }
        return true;
    }

    private Date parseDate(String isoDate, boolean startOfDay) {
        if (!StringUtils.hasText(isoDate)) {
            return null;
        }
        LocalDate ld = LocalDate.parse(isoDate);
        if (startOfDay) {
            return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        return Date.from(ld.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private String readString(Object target, String getterName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(getterName);
            Object value = method.invoke(target);
            return value != null ? String.valueOf(value) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void invokeSetterIfExists(Object target, String methodName, Class<?> paramType, Object value) {
        if (target == null) {
            return;
        }
        try {
            Method method = target.getClass().getMethod(methodName, paramType);
            method.invoke(target, value);
        } catch (Exception ignored) {
        }
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private static class RevenueAccumulator {
        private final String planId;
        private String planCode;
        private String planName;
        private double revenue;
        private int subscriptionCount;

        private RevenueAccumulator(String planId) {
            this.planId = planId;
        }
    }
}