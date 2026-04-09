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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
        PlatformRevenueAnalyticsRequest request = objectMapper.convertValue(payload, PlatformRevenueAnalyticsRequest.class);

        Date startDate = PlatformAnalyticsSupport.parseDate(request.getStartDate(), true);
        Date endDate = PlatformAnalyticsSupport.parseDate(request.getEndDate(), false);

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

            RevenueAccumulator acc = byPlan.computeIfAbsent(sub.getPlanId(), RevenueAccumulator::new);
            acc.planCode = PlatformAnalyticsSupport.planCode(plan);
            acc.planName = PlatformAnalyticsSupport.planName(plan);
            acc.subscriptionCount++;
            acc.revenue += monthlyRevenue;
        }

        for (InvoiceEntity inv : allInvoices) {
            if (inv != null && STATUS_PAID.equalsIgnoreCase(inv.getStatus())
                    && PlatformAnalyticsSupport.inRange(inv.getCreatedAt(), startDate, endDate)) {
                totalRevenue += inv.getAmountTotal() != null ? inv.getAmountTotal() : 0.0;
            }
        }

        List<PlatformRevenueAnalyticsResponse.RevenueByPlanItem> revenueByPlans = new ArrayList<>();
        for (RevenueAccumulator acc : byPlan.values()) {
            PlatformRevenueAnalyticsResponse.RevenueByPlanItem item = new PlatformRevenueAnalyticsResponse.RevenueByPlanItem();
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

    private void invokeSetterIfExists(Object target, String methodName, Class<?> paramType, Object value) {
        try {
            Method method = target.getClass().getMethod(methodName, paramType);
            method.invoke(target, value);
        } catch (Exception ignored) {
        }
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
