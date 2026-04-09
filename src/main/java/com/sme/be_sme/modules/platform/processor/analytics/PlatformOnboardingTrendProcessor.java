package com.sme.be_sme.modules.platform.processor.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformOnboardingTrendRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformOnboardingTrendResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformOnboardingTrendProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformOnboardingTrendRequest request = objectMapper.convertValue(payload, PlatformOnboardingTrendRequest.class);
        String groupBy = PlatformAnalyticsSupport.normalizeGroupBy(request.getGroupBy());
        LocalDate start = PlatformAnalyticsSupport.parseLocalDate(request.getStartDate(), LocalDate.now().minusMonths(11));
        LocalDate end = PlatformAnalyticsSupport.parseLocalDate(request.getEndDate(), LocalDate.now());
        LocalDate previousStart = PlatformAnalyticsSupport.previousPeriodStart(start, end, groupBy);
        LocalDate previousEnd = PlatformAnalyticsSupport.previousPeriodEnd(start, end, groupBy);
        long distance = PlatformAnalyticsSupport.bucketDistance(start, end, groupBy) + 1;

        List<String> buckets = PlatformAnalyticsSupport.buildBuckets(start, end, groupBy);
        Map<String, Stats> current = new HashMap<>();
        Map<String, Integer> previousTotal = PlatformAnalyticsSupport.seedIntMap(buckets);
        for (String bucket : buckets) {
            current.put(bucket, new Stats());
        }

        java.util.Date startDate = PlatformAnalyticsSupport.parseDate(start.toString(), true);
        java.util.Date endDate = PlatformAnalyticsSupport.parseDate(end.toString(), false);
        java.util.Date previousStartDate = PlatformAnalyticsSupport.parseDate(previousStart.toString(), true);
        java.util.Date previousEndDate = PlatformAnalyticsSupport.parseDate(previousEnd.toString(), false);

        for (OnboardingInstanceEntity item : onboardingInstanceMapper.selectAll()) {
            if (item == null || item.getCreatedAt() == null) {
                continue;
            }
            if (PlatformAnalyticsSupport.inRange(item.getCreatedAt(), startDate, endDate)) {
                String bucket = PlatformAnalyticsSupport.bucketOf(item.getCreatedAt(), groupBy);
                Stats stats = current.get(bucket);
                if (stats != null) {
                    stats.total++;
                    if ("COMPLETED".equalsIgnoreCase(item.getStatus())) {
                        stats.completed++;
                    } else if ("OVERDUE".equalsIgnoreCase(item.getStatus()) || "RISK".equalsIgnoreCase(item.getStatus())) {
                        stats.risk++;
                        stats.active++;
                    } else {
                        stats.active++;
                    }
                }
            }
            if (Boolean.TRUE.equals(request.getComparePrevious())
                    && PlatformAnalyticsSupport.inRange(item.getCreatedAt(), previousStartDate, previousEndDate)) {
                String previousBucket = PlatformAnalyticsSupport.bucketOf(item.getCreatedAt(), groupBy);
                LocalDate shifted = PlatformAnalyticsSupport.step(PlatformAnalyticsSupport.bucketToDate(previousBucket, groupBy), groupBy, distance);
                String shiftedBucket = PlatformAnalyticsSupport.formatBucket(shifted, groupBy);
                previousTotal.computeIfPresent(shiftedBucket, (k, v) -> v + 1);
            }
        }

        List<PlatformOnboardingTrendResponse.TrendItem> items = new ArrayList<>();
        for (String bucket : buckets) {
            Stats stats = current.get(bucket);
            PlatformOnboardingTrendResponse.TrendItem item = new PlatformOnboardingTrendResponse.TrendItem();
            item.setBucket(bucket);
            item.setTotal(stats.total);
            item.setActive(stats.active);
            item.setCompleted(stats.completed);
            item.setRisk(stats.risk);
            item.setPreviousTotal(Boolean.TRUE.equals(request.getComparePrevious()) ? previousTotal.getOrDefault(bucket, 0) : null);
            item.setGrowthRate(Boolean.TRUE.equals(request.getComparePrevious()) ? PlatformAnalyticsSupport.growth(stats.total, item.getPreviousTotal()) : null);
            items.add(item);
        }

        PlatformOnboardingTrendResponse response = new PlatformOnboardingTrendResponse();
        response.setStartDate(start.toString());
        response.setEndDate(end.toString());
        response.setGroupBy(groupBy);
        response.setItems(items);
        return response;
    }

    private static class Stats {
        private int total;
        private int active;
        private int completed;
        private int risk;
    }
}
