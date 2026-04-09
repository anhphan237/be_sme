package com.sme.be_sme.modules.platform.processor.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformEmployeeTrendRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformEmployeeTrendResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformEmployeeTrendProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformEmployeeTrendRequest request = objectMapper.convertValue(payload, PlatformEmployeeTrendRequest.class);
        String groupBy = PlatformAnalyticsSupport.normalizeGroupBy(request.getGroupBy());
        LocalDate start = PlatformAnalyticsSupport.parseLocalDate(request.getStartDate(), LocalDate.now().minusMonths(11));
        LocalDate end = PlatformAnalyticsSupport.parseLocalDate(request.getEndDate(), LocalDate.now());
        LocalDate previousStart = PlatformAnalyticsSupport.previousPeriodStart(start, end, groupBy);
        LocalDate previousEnd = PlatformAnalyticsSupport.previousPeriodEnd(start, end, groupBy);
        long distance = PlatformAnalyticsSupport.bucketDistance(start, end, groupBy) + 1;

        List<String> buckets = PlatformAnalyticsSupport.buildBuckets(start, end, groupBy);
        Map<String, Integer> current = PlatformAnalyticsSupport.seedIntMap(buckets);
        Map<String, Integer> previous = PlatformAnalyticsSupport.seedIntMap(buckets);

        java.util.Date startDate = PlatformAnalyticsSupport.parseDate(start.toString(), true);
        java.util.Date endDate = PlatformAnalyticsSupport.parseDate(end.toString(), false);
        java.util.Date previousStartDate = PlatformAnalyticsSupport.parseDate(previousStart.toString(), true);
        java.util.Date previousEndDate = PlatformAnalyticsSupport.parseDate(previousEnd.toString(), false);

        for (UserEntity user : userMapper.selectAll()) {
            if (user == null || user.getCreatedAt() == null || !PlatformAnalyticsSupport.isEmployee(user)) {
                continue;
            }
            if (PlatformAnalyticsSupport.inRange(user.getCreatedAt(), startDate, endDate)) {
                String bucket = PlatformAnalyticsSupport.bucketOf(user.getCreatedAt(), groupBy);
                current.computeIfPresent(bucket, (k, v) -> v + 1);
            }
            if (Boolean.TRUE.equals(request.getComparePrevious())
                    && PlatformAnalyticsSupport.inRange(user.getCreatedAt(), previousStartDate, previousEndDate)) {
                String previousBucket = PlatformAnalyticsSupport.bucketOf(user.getCreatedAt(), groupBy);
                LocalDate shifted = PlatformAnalyticsSupport.step(PlatformAnalyticsSupport.bucketToDate(previousBucket, groupBy), groupBy, distance);
                String shiftedBucket = PlatformAnalyticsSupport.formatBucket(shifted, groupBy);
                previous.computeIfPresent(shiftedBucket, (k, v) -> v + 1);
            }
        }

        List<PlatformEmployeeTrendResponse.TrendItem> items = new ArrayList<>();
        for (String bucket : buckets) {
            PlatformEmployeeTrendResponse.TrendItem item = new PlatformEmployeeTrendResponse.TrendItem();
            item.setBucket(bucket);
            item.setValue(current.getOrDefault(bucket, 0));
            item.setPreviousValue(Boolean.TRUE.equals(request.getComparePrevious()) ? previous.getOrDefault(bucket, 0) : null);
            item.setGrowthRate(Boolean.TRUE.equals(request.getComparePrevious()) ? PlatformAnalyticsSupport.growth(item.getValue(), item.getPreviousValue()) : null);
            items.add(item);
        }

        PlatformEmployeeTrendResponse response = new PlatformEmployeeTrendResponse();
        response.setStartDate(start.toString());
        response.setEndDate(end.toString());
        response.setGroupBy(groupBy);
        response.setItems(items);
        return response;
    }
}
