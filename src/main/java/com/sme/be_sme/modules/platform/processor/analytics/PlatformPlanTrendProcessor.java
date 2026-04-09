package com.sme.be_sme.modules.platform.processor.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.PlanMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.PlanEntity;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformPlanTrendRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformPlanTrendResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformPlanTrendProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final PlanMapper planMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformPlanTrendRequest request = objectMapper.convertValue(payload, PlatformPlanTrendRequest.class);
        String groupBy = PlatformAnalyticsSupport.normalizeGroupBy(request.getGroupBy());
        LocalDate start = PlatformAnalyticsSupport.parseLocalDate(request.getStartDate(), LocalDate.now().minusMonths(11));
        LocalDate end = PlatformAnalyticsSupport.parseLocalDate(request.getEndDate(), LocalDate.now());
        LocalDate previousStart = PlatformAnalyticsSupport.previousPeriodStart(start, end, groupBy);
        LocalDate previousEnd = PlatformAnalyticsSupport.previousPeriodEnd(start, end, groupBy);
        long distance = PlatformAnalyticsSupport.bucketDistance(start, end, groupBy) + 1;

        List<String> buckets = PlatformAnalyticsSupport.buildBuckets(start, end, groupBy);
        java.util.Date startDate = PlatformAnalyticsSupport.parseDate(start.toString(), true);
        java.util.Date endDate = PlatformAnalyticsSupport.parseDate(end.toString(), false);
        java.util.Date previousStartDate = PlatformAnalyticsSupport.parseDate(previousStart.toString(), true);
        java.util.Date previousEndDate = PlatformAnalyticsSupport.parseDate(previousEnd.toString(), false);

        Map<String, PlanEntity> plansById = new HashMap<>();
        for (PlanEntity plan : planMapper.selectAll()) {
            if (plan != null && plan.getPlanId() != null) {
                plansById.put(plan.getPlanId(), plan);
            }
        }

        Map<String, Map<String, Integer>> current = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> previous = new LinkedHashMap<>();
        for (String bucket : buckets) {
            current.put(bucket, new LinkedHashMap<>());
            previous.put(bucket, new LinkedHashMap<>());
        }

        for (SubscriptionEntity sub : subscriptionMapper.selectAll()) {
            if (sub == null || sub.getCreatedAt() == null || sub.getPlanId() == null) {
                continue;
            }
            if (PlatformAnalyticsSupport.inRange(sub.getCreatedAt(), startDate, endDate)) {
                String bucket = PlatformAnalyticsSupport.bucketOf(sub.getCreatedAt(), groupBy);
                current.get(bucket).merge(sub.getPlanId(), 1, Integer::sum);
            }
            if (Boolean.TRUE.equals(request.getComparePrevious())
                    && PlatformAnalyticsSupport.inRange(sub.getCreatedAt(), previousStartDate, previousEndDate)) {
                String previousBucket = PlatformAnalyticsSupport.bucketOf(sub.getCreatedAt(), groupBy);
                LocalDate shifted = PlatformAnalyticsSupport.step(PlatformAnalyticsSupport.bucketToDate(previousBucket, groupBy), groupBy, distance);
                String shiftedBucket = PlatformAnalyticsSupport.formatBucket(shifted, groupBy);
                if (previous.containsKey(shiftedBucket)) {
                    previous.get(shiftedBucket).merge(sub.getPlanId(), 1, Integer::sum);
                }
            }
        }

        List<PlatformPlanTrendResponse.PlanTrendItem> items = new ArrayList<>();
        for (String bucket : buckets) {
            PlatformPlanTrendResponse.PlanTrendItem item = new PlatformPlanTrendResponse.PlanTrendItem();
            item.setBucket(bucket);
            List<PlatformPlanTrendResponse.PlanValueItem> plans = new ArrayList<>();
            Map<String, Integer> currentValues = current.get(bucket);
            for (PlanEntity plan : plansById.values()) {
                int value = currentValues.getOrDefault(plan.getPlanId(), 0);
                int previousValue = previous.get(bucket).getOrDefault(plan.getPlanId(), 0);
                PlatformPlanTrendResponse.PlanValueItem p = new PlatformPlanTrendResponse.PlanValueItem();
                p.setPlanId(plan.getPlanId());
                p.setPlanCode(PlatformAnalyticsSupport.planCode(plan));
                p.setPlanName(PlatformAnalyticsSupport.planName(plan));
                p.setValue(value);
                p.setPreviousValue(Boolean.TRUE.equals(request.getComparePrevious()) ? previousValue : null);
                p.setGrowthRate(Boolean.TRUE.equals(request.getComparePrevious()) ? PlatformAnalyticsSupport.growth(value, previousValue) : null);
                plans.add(p);
            }
            item.setPlans(plans);
            items.add(item);
        }

        PlatformPlanTrendResponse response = new PlatformPlanTrendResponse();
        response.setStartDate(start.toString());
        response.setEndDate(end.toString());
        response.setGroupBy(groupBy);
        response.setItems(items);
        return response;
    }
}
