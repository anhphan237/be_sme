package com.sme.be_sme.modules.platform.processor.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformCompanyTrendRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformCompanyTrendResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformCompanyTrendProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final CompanyMapper companyMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformCompanyTrendRequest request = objectMapper.convertValue(payload, PlatformCompanyTrendRequest.class);
        String groupBy = PlatformAnalyticsSupport.normalizeGroupBy(request.getGroupBy());
        LocalDate start = PlatformAnalyticsSupport.parseLocalDate(request.getStartDate(), LocalDate.now().minusMonths(11));
        LocalDate end = PlatformAnalyticsSupport.parseLocalDate(request.getEndDate(), LocalDate.now());
        LocalDate previousStart = PlatformAnalyticsSupport.previousPeriodStart(start, end, groupBy);
        LocalDate previousEnd = PlatformAnalyticsSupport.previousPeriodEnd(start, end, groupBy);

        List<String> buckets = PlatformAnalyticsSupport.buildBuckets(start, end, groupBy);
        Map<String, Integer> currentMap = PlatformAnalyticsSupport.seedIntMap(buckets);
        Map<String, Integer> previousMapByShiftedBucket = PlatformAnalyticsSupport.seedIntMap(buckets);

        java.util.Date startDate = PlatformAnalyticsSupport.parseDate(start.toString(), true);
        java.util.Date endDate = PlatformAnalyticsSupport.parseDate(end.toString(), false);
        java.util.Date previousStartDate = PlatformAnalyticsSupport.parseDate(previousStart.toString(), true);
        java.util.Date previousEndDate = PlatformAnalyticsSupport.parseDate(previousEnd.toString(), false);
        long distance = PlatformAnalyticsSupport.bucketDistance(start, end, groupBy) + 1;

        for (CompanyEntity company : companyMapper.selectAll()) {
            if (company == null || company.getCreatedAt() == null || "PLATFORM".equalsIgnoreCase(company.getStatus())) {
                continue;
            }
            if (StringUtils.hasText(request.getStatus()) && !request.getStatus().equalsIgnoreCase(company.getStatus())) {
                continue;
            }
            if (PlatformAnalyticsSupport.inRange(company.getCreatedAt(), startDate, endDate)) {
                String bucket = PlatformAnalyticsSupport.bucketOf(company.getCreatedAt(), groupBy);
                currentMap.computeIfPresent(bucket, (k, v) -> v + 1);
            }
            if (Boolean.TRUE.equals(request.getComparePrevious())
                    && PlatformAnalyticsSupport.inRange(company.getCreatedAt(), previousStartDate, previousEndDate)) {
                String previousBucket = PlatformAnalyticsSupport.bucketOf(company.getCreatedAt(), groupBy);
                LocalDate shifted = PlatformAnalyticsSupport.step(PlatformAnalyticsSupport.bucketToDate(previousBucket, groupBy), groupBy, distance);
                String shiftedBucket = PlatformAnalyticsSupport.formatBucket(shifted, groupBy);
                previousMapByShiftedBucket.computeIfPresent(shiftedBucket, (k, v) -> v + 1);
            }
        }

        List<PlatformCompanyTrendResponse.TrendItem> items = new ArrayList<>();
        for (String bucket : buckets) {
            PlatformCompanyTrendResponse.TrendItem item = new PlatformCompanyTrendResponse.TrendItem();
            item.setBucket(bucket);
            item.setValue(currentMap.getOrDefault(bucket, 0));
            item.setPreviousValue(Boolean.TRUE.equals(request.getComparePrevious()) ? previousMapByShiftedBucket.getOrDefault(bucket, 0) : null);
            item.setGrowthRate(Boolean.TRUE.equals(request.getComparePrevious())
                    ? PlatformAnalyticsSupport.growth(item.getValue(), item.getPreviousValue()) : null);
            items.add(item);
        }

        PlatformCompanyTrendResponse response = new PlatformCompanyTrendResponse();
        response.setStartDate(start.toString());
        response.setEndDate(end.toString());
        response.setGroupBy(groupBy);
        response.setItems(items);
        return response;
    }
}
