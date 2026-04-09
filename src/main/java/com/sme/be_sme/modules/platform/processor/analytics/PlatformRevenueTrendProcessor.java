package com.sme.be_sme.modules.platform.processor.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.InvoiceEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformRevenueTrendRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformRevenueTrendResponse;
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
public class PlatformRevenueTrendProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final InvoiceMapper invoiceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformRevenueTrendRequest request = objectMapper.convertValue(payload, PlatformRevenueTrendRequest.class);
        String groupBy = PlatformAnalyticsSupport.normalizeGroupBy(request.getGroupBy());
        LocalDate start = PlatformAnalyticsSupport.parseLocalDate(request.getStartDate(), LocalDate.now().minusMonths(11));
        LocalDate end = PlatformAnalyticsSupport.parseLocalDate(request.getEndDate(), LocalDate.now());
        LocalDate previousStart = PlatformAnalyticsSupport.previousPeriodStart(start, end, groupBy);
        LocalDate previousEnd = PlatformAnalyticsSupport.previousPeriodEnd(start, end, groupBy);

        List<String> buckets = PlatformAnalyticsSupport.buildBuckets(start, end, groupBy);
        Map<String, Double> currentMap = PlatformAnalyticsSupport.seedDoubleMap(buckets);
        Map<String, Double> previousMap = PlatformAnalyticsSupport.seedDoubleMap(buckets);
        long distance = PlatformAnalyticsSupport.bucketDistance(start, end, groupBy) + 1;

        java.util.Date startDate = PlatformAnalyticsSupport.parseDate(start.toString(), true);
        java.util.Date endDate = PlatformAnalyticsSupport.parseDate(end.toString(), false);
        java.util.Date previousStartDate = PlatformAnalyticsSupport.parseDate(previousStart.toString(), true);
        java.util.Date previousEndDate = PlatformAnalyticsSupport.parseDate(previousEnd.toString(), false);

        for (InvoiceEntity invoice : invoiceMapper.selectAll()) {
            if (invoice == null || invoice.getCreatedAt() == null || !"PAID".equalsIgnoreCase(invoice.getStatus())) {
                continue;
            }
            double amount = invoice.getAmountTotal() != null ? invoice.getAmountTotal() : 0.0;
            if (PlatformAnalyticsSupport.inRange(invoice.getCreatedAt(), startDate, endDate)) {
                String bucket = PlatformAnalyticsSupport.bucketOf(invoice.getCreatedAt(), groupBy);
                currentMap.computeIfPresent(bucket, (k, v) -> v + amount);
            }
            if (Boolean.TRUE.equals(request.getComparePrevious())
                    && PlatformAnalyticsSupport.inRange(invoice.getCreatedAt(), previousStartDate, previousEndDate)) {
                String previousBucket = PlatformAnalyticsSupport.bucketOf(invoice.getCreatedAt(), groupBy);
                LocalDate shifted = PlatformAnalyticsSupport.step(PlatformAnalyticsSupport.bucketToDate(previousBucket, groupBy), groupBy, distance);
                String shiftedBucket = PlatformAnalyticsSupport.formatBucket(shifted, groupBy);
                previousMap.computeIfPresent(shiftedBucket, (k, v) -> v + amount);
            }
        }

        List<PlatformRevenueTrendResponse.TrendItem> items = new ArrayList<>();
        for (String bucket : buckets) {
            PlatformRevenueTrendResponse.TrendItem item = new PlatformRevenueTrendResponse.TrendItem();
            item.setBucket(bucket);
            item.setValue(currentMap.getOrDefault(bucket, 0.0));
            item.setPreviousValue(Boolean.TRUE.equals(request.getComparePrevious()) ? previousMap.getOrDefault(bucket, 0.0) : null);
            item.setGrowthRate(Boolean.TRUE.equals(request.getComparePrevious())
                    ? PlatformAnalyticsSupport.growth(item.getValue(), item.getPreviousValue()) : null);
            items.add(item);
        }

        PlatformRevenueTrendResponse response = new PlatformRevenueTrendResponse();
        response.setStartDate(start.toString());
        response.setEndDate(end.toString());
        response.setGroupBy(groupBy);
        response.setItems(items);
        return response;
    }
}
