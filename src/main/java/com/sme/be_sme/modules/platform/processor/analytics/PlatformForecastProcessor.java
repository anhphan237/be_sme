package com.sme.be_sme.modules.platform.processor.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.InvoiceMapper;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.platform.api.request.PlatformForecastRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformForecastResponse;
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
public class PlatformForecastProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final CompanyMapper companyMapper;
    private final InvoiceMapper invoiceMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final UserMapper userMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformForecastRequest request = objectMapper.convertValue(payload, PlatformForecastRequest.class);
        String metric = request.getMetric() != null ? request.getMetric().toUpperCase() : "COMPANY";
        String groupBy = PlatformAnalyticsSupport.normalizeGroupBy(request.getGroupBy());
        int defaultForecastPoints = "REVENUE".equals(metric) ? 12 : 3;
        int forecastPoints = request.getForecastPoints() != null && request.getForecastPoints() > 0
                ? request.getForecastPoints()
                : defaultForecastPoints;

        LocalDate start = PlatformAnalyticsSupport.parseLocalDate(request.getStartDate(), LocalDate.now().minusMonths(11));
        LocalDate end = PlatformAnalyticsSupport.parseLocalDate(request.getEndDate(), LocalDate.now());
        List<String> buckets = PlatformAnalyticsSupport.buildBuckets(start, end, groupBy);
        Map<String, Double> values = PlatformAnalyticsSupport.seedDoubleMap(buckets);

        java.util.Date startDate = PlatformAnalyticsSupport.parseDate(start.toString(), true);
        java.util.Date endDate = PlatformAnalyticsSupport.parseDate(end.toString(), false);

        if ("COMPANY".equals(metric)) {
            companyMapper.selectAll().forEach(company -> {
                if (company != null && company.getCreatedAt() != null && !"PLATFORM".equalsIgnoreCase(company.getStatus())
                        && PlatformAnalyticsSupport.inRange(company.getCreatedAt(), startDate, endDate)) {
                    String bucket = PlatformAnalyticsSupport.bucketOf(company.getCreatedAt(), groupBy);
                    values.computeIfPresent(bucket, (k, v) -> v + 1.0);
                }
            });
        } else if ("REVENUE".equals(metric)) {
            invoiceMapper.selectAll().forEach(inv -> {
                if (inv != null && inv.getCreatedAt() != null && "PAID".equalsIgnoreCase(inv.getStatus())
                        && PlatformAnalyticsSupport.inRange(inv.getCreatedAt(), startDate, endDate)) {
                    String bucket = PlatformAnalyticsSupport.bucketOf(inv.getCreatedAt(), groupBy);
                    values.computeIfPresent(bucket, (k, v) -> v + (inv.getAmountTotal() != null ? inv.getAmountTotal() : 0.0));
                }
            });
        } else if ("ONBOARDING".equals(metric)) {
            onboardingInstanceMapper.selectAll().forEach(item -> {
                if (item != null && item.getCreatedAt() != null && PlatformAnalyticsSupport.inRange(item.getCreatedAt(), startDate, endDate)) {
                    String bucket = PlatformAnalyticsSupport.bucketOf(item.getCreatedAt(), groupBy);
                    values.computeIfPresent(bucket, (k, v) -> v + 1.0);
                }
            });
        } else if ("EMPLOYEE".equals(metric)) {
            userMapper.selectAll().forEach(user -> {
                if (user != null && user.getCreatedAt() != null && PlatformAnalyticsSupport.isEmployee(user)
                        && PlatformAnalyticsSupport.inRange(user.getCreatedAt(), startDate, endDate)) {
                    String bucket = PlatformAnalyticsSupport.bucketOf(user.getCreatedAt(), groupBy);
                    values.computeIfPresent(bucket, (k, v) -> v + 1.0);
                }
            });
        }

        List<PlatformForecastResponse.PointItem> historical = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        int index = 0;
        double sumX = 0.0, sumY = 0.0, sumXY = 0.0, sumX2 = 0.0;
        for (String bucket : buckets) {
            double value = values.getOrDefault(bucket, 0.0);
            PlatformForecastResponse.PointItem point = new PlatformForecastResponse.PointItem();
            point.setBucket(bucket);
            point.setValue(value);
            historical.add(point);
            y.add(value);
            sumX += index;
            sumY += value;
            sumXY += index * value;
            sumX2 += index * index;
            index++;
        }

        int n = buckets.size();
        double denominator = n * sumX2 - sumX * sumX;
        double slope = denominator == 0 ? 0.0 : (n * sumXY - sumX * sumY) / denominator;
        double intercept = n == 0 ? 0.0 : (sumY - slope * sumX) / n;

        List<PlatformForecastResponse.PointItem> forecast = new ArrayList<>();
        LocalDate cursor = PlatformAnalyticsSupport.alignStart(end, groupBy);
        String method = "LINEAR_REGRESSION";
        List<Double> forecastValues;
        if ("REVENUE".equals(metric)) {
            forecastValues = holtLinearForecast(y, forecastPoints);
            method = "HOLT_LINEAR";
        } else {
            forecastValues = linearRegressionForecast(intercept, slope, n, forecastPoints);
        }
        for (int i = 1; i <= forecastPoints; i++) {
            cursor = PlatformAnalyticsSupport.step(cursor, groupBy, 1);
            PlatformForecastResponse.PointItem point = new PlatformForecastResponse.PointItem();
            point.setBucket(PlatformAnalyticsSupport.formatBucket(cursor, groupBy));
            point.setValue(Math.max(0.0, forecastValues.get(i - 1)));
            forecast.add(point);
        }

        PlatformForecastResponse response = new PlatformForecastResponse();
        response.setMetric(metric);
        response.setGroupBy(groupBy);
        response.setMethod(method);
        response.setConfidenceNote("Forecast is directional and based on historical trend only.");
        response.setHistorical(historical);
        response.setForecast(forecast);
        return response;
    }

    private List<Double> linearRegressionForecast(double intercept, double slope, int historicalSize, int forecastPoints) {
        List<Double> output = new ArrayList<>();
        for (int i = 1; i <= forecastPoints; i++) {
            output.add(intercept + slope * (historicalSize - 1 + i));
        }
        return output;
    }

    /**
     * Holt's linear trend (double exponential smoothing).
     * Alpha/Beta kept fixed for stable and predictable behavior.
     */
    private List<Double> holtLinearForecast(List<Double> series, int forecastPoints) {
        List<Double> output = new ArrayList<>();
        if (series == null || series.isEmpty()) {
            for (int i = 0; i < forecastPoints; i++) {
                output.add(0.0);
            }
            return output;
        }
        if (series.size() == 1) {
            for (int i = 0; i < forecastPoints; i++) {
                output.add(series.get(0));
            }
            return output;
        }

        final double alpha = 0.6;
        final double beta = 0.3;

        double level = series.get(0);
        double trend = series.get(1) - series.get(0);

        for (int t = 1; t < series.size(); t++) {
            double value = series.get(t);
            double prevLevel = level;
            level = alpha * value + (1 - alpha) * (level + trend);
            trend = beta * (level - prevLevel) + (1 - beta) * trend;
        }

        for (int i = 1; i <= forecastPoints; i++) {
            output.add(level + i * trend);
        }
        return output;
    }
}
