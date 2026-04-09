package com.sme.be_sme.modules.platform.processor.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformPlanDistributionRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformPlanDistributionResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformPlanDistributionProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;
    private final UserMapper userMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformPlanDistributionRequest request = objectMapper.convertValue(payload, PlatformPlanDistributionRequest.class);
        java.util.Date startDate = PlatformAnalyticsSupport.parseDate(request.getStartDate(), true);
        java.util.Date endDate = PlatformAnalyticsSupport.parseDate(request.getEndDate(), false);

        Map<String, PlanEntity> planById = new HashMap<>();
        for (PlanEntity plan : planMapper.selectAll()) {
            if (plan != null && plan.getPlanId() != null) {
                planById.put(plan.getPlanId(), plan);
            }
        }

        Map<String, DistributionAccumulator> byPlan = new HashMap<>();
        Map<String, String> companyToPlan = new HashMap<>();

        for (SubscriptionEntity sub : subscriptionMapper.selectAll()) {
            if (sub == null || sub.getPlanId() == null || !PlatformAnalyticsSupport.inRange(sub.getCreatedAt(), startDate, endDate)) {
                continue;
            }
            PlanEntity plan = planById.get(sub.getPlanId());
            DistributionAccumulator acc = byPlan.computeIfAbsent(sub.getPlanId(), k -> new DistributionAccumulator());
            acc.planId = sub.getPlanId();
            acc.planCode = plan != null ? PlatformAnalyticsSupport.planCode(plan) : null;
            acc.planName = plan != null ? PlatformAnalyticsSupport.planName(plan) : null;
            acc.subscriptionCount++;
            acc.companyIds.add(sub.getCompanyId());
            companyToPlan.put(sub.getCompanyId(), sub.getPlanId());
            if ("YEARLY".equalsIgnoreCase(sub.getBillingCycle())) {
                acc.mrr += plan != null && plan.getPriceVndYearly() != null ? plan.getPriceVndYearly() / 12.0 : 0.0;
            } else {
                acc.mrr += plan != null && plan.getPriceVndMonthly() != null ? plan.getPriceVndMonthly() : 0.0;
            }
        }

        int totalEmployees = 0;
        for (UserEntity user : userMapper.selectAll()) {
            if (user == null || !PlatformAnalyticsSupport.isEmployee(user)) {
                continue;
            }
            totalEmployees++;
            String planId = companyToPlan.get(user.getCompanyId());
            if (planId != null) {
                byPlan.computeIfAbsent(planId, k -> new DistributionAccumulator()).employeeCount++;
            }
        }

        int totalCompanies = 0;
        double totalMrr = 0.0;
        for (DistributionAccumulator acc : byPlan.values()) {
            totalCompanies += acc.companyIds.size();
            totalMrr += acc.mrr;
        }

        List<PlatformPlanDistributionResponse.PlanDistributionItem> items = new ArrayList<>();
        for (DistributionAccumulator acc : byPlan.values()) {
            PlatformPlanDistributionResponse.PlanDistributionItem item = new PlatformPlanDistributionResponse.PlanDistributionItem();
            item.setPlanId(acc.planId);
            item.setPlanCode(acc.planCode);
            item.setPlanName(acc.planName);
            item.setCompanyCount(acc.companyIds.size());
            item.setSubscriptionCount(acc.subscriptionCount);
            item.setEmployeeCount(acc.employeeCount);
            item.setMrr(acc.mrr);
            item.setPercentage(totalCompanies > 0 ? (double) acc.companyIds.size() / totalCompanies : 0.0);
            items.add(item);
        }

        PlatformPlanDistributionResponse response = new PlatformPlanDistributionResponse();
        response.setTotalCompanies(totalCompanies);
        response.setTotalEmployees(totalEmployees);
        response.setTotalMrr(totalMrr);
        response.setItems(items);
        return response;
    }

    private static class DistributionAccumulator {
        private String planId;
        private String planCode;
        private String planName;
        private int subscriptionCount;
        private int employeeCount;
        private double mrr;
        private final Set<String> companyIds = new HashSet<>();
    }
}
