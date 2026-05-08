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
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformForecastProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final CompanyMapper companyMapper;
    private final InvoiceMapper invoiceMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final UserMapper userMapper;
    private final ChatLanguageModel chatModel;

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
            List<Double> aiForecastValues = geminiRevenueForecast(y, buckets, groupBy, forecastPoints);
            if (!aiForecastValues.isEmpty()) {
                forecastValues = aiForecastValues;
                method = "GEMINI_TREND";
            } else {
                forecastValues = holtLinearForecast(y, forecastPoints);
                method = "HOLT_LINEAR";
            }
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

    private List<Double> geminiRevenueForecast(List<Double> series, List<String> buckets, String groupBy, int forecastPoints) {
        if (series == null || series.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            String prompt = buildRevenueForecastPrompt(series, buckets, groupBy, forecastPoints);
            String raw = chatModel.generate(prompt);
            return parseForecastValues(raw, forecastPoints);
        } catch (Exception ex) {
            log.warn("Gemini revenue forecast failed, fallback to Holt. reason={}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private String buildRevenueForecastPrompt(List<Double> series, List<String> buckets, String groupBy, int forecastPoints) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a revenue forecasting assistant.\n");
        sb.append("Task: forecast next ").append(forecastPoints).append(" ").append(groupBy).append(" points from historical revenue trend.\n");
        sb.append("Historical buckets: ").append(buckets).append("\n");
        sb.append("Historical values: ").append(series).append("\n");
        sb.append("Rules:\n");
        sb.append("- Return ONLY a JSON array of ").append(forecastPoints).append(" non-negative numbers.\n");
        sb.append("- No markdown, no explanation, no extra keys.\n");
        sb.append("- Keep trend realistic and continuous from recent points.\n");
        return sb.toString();
    }

    private List<Double> parseForecastValues(String raw, int forecastPoints) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        String text = raw.trim();
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return Collections.emptyList();
        }
        String arrayText = text.substring(start, end + 1);
        List<Double> parsed = new ArrayList<>();
        try {
            JsonNode array = objectMapper.readTree(arrayText);
            if (!array.isArray()) {
                return Collections.emptyList();
            }
            for (JsonNode item : array) {
                if (!item.isNumber()) {
                    continue;
                }
                parsed.add(Math.max(0.0, item.asDouble()));
                if (parsed.size() == forecastPoints) {
                    break;
                }
            }
        } catch (Exception ex) {
            return Collections.emptyList();
        }
        if (parsed.size() != forecastPoints) {
            return Collections.emptyList();
        }
        return parsed;
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
