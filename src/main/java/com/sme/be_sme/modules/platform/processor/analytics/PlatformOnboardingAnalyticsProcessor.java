package com.sme.be_sme.modules.platform.processor.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.infrastructure.mapper.CompanyMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.CompanyEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformOnboardingAnalyticsRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformOnboardingAnalyticsResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformOnboardingAnalyticsProcessor extends BaseBizProcessor<BizContext> {

    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_OVERDUE = "OVERDUE";
    private static final String STATUS_RISK = "RISK";

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final CompanyMapper companyMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformOnboardingAnalyticsRequest request = objectMapper.convertValue(
                payload,
                PlatformOnboardingAnalyticsRequest.class
        );

        Date startDate = PlatformAnalyticsSupport.parseDate(request.getStartDate(), true);
        Date endDate = PlatformAnalyticsSupport.parseDate(request.getEndDate(), false);

        List<OnboardingInstanceEntity> filtered = new ArrayList<>();
        for (OnboardingInstanceEntity instance : onboardingInstanceMapper.selectAll()) {
            if (instance != null && PlatformAnalyticsSupport.inRange(instance.getCreatedAt(), startDate, endDate)) {
                filtered.add(instance);
            }
        }

        int totalOnboardings = filtered.size();
        int completedOnboardings = 0;
        long totalCompletionDays = 0L;
        int completedWithDates = 0;

        Map<String, CompanyStats> statsByCompanyId = new HashMap<>();
        Map<String, String> companyNameById = new HashMap<>();

        for (CompanyEntity company : companyMapper.selectAll()) {
            if (company != null && company.getCompanyId() != null) {
                companyNameById.put(company.getCompanyId(), company.getName());
            }
        }

        for (OnboardingInstanceEntity instance : filtered) {
            String companyId = instance.getCompanyId();
            if (companyId == null) {
                companyId = "UNKNOWN";
            }

            CompanyStats companyStats = statsByCompanyId.computeIfAbsent(
                    companyId,
                    key -> new CompanyStats()
            );

            companyStats.total++;

            if (STATUS_COMPLETED.equalsIgnoreCase(instance.getStatus())) {
                completedOnboardings++;
                companyStats.completed++;

                if (instance.getCompletedAt() != null && instance.getCreatedAt() != null) {
                    long diffMs = instance.getCompletedAt().getTime() - instance.getCreatedAt().getTime();
                    long diffDays = TimeUnit.MILLISECONDS.toDays(diffMs);

                    totalCompletionDays += diffDays;
                    completedWithDates++;

                    companyStats.totalCompletionDays += diffDays;
                    companyStats.completedWithDates++;
                }
            } else if (STATUS_CANCELLED.equalsIgnoreCase(instance.getStatus())) {
                companyStats.cancelled++;
            } else if (STATUS_OVERDUE.equalsIgnoreCase(instance.getStatus())
                    || STATUS_RISK.equalsIgnoreCase(instance.getStatus())) {
                companyStats.risk++;
            }
        }

        PlatformOnboardingAnalyticsResponse response = new PlatformOnboardingAnalyticsResponse();
        response.setTotalOnboardings(totalOnboardings);
        response.setCompletedOnboardings(completedOnboardings);
        response.setCompletionRate(totalOnboardings > 0
                ? (double) completedOnboardings / totalOnboardings
                : 0.0);
        response.setAverageCompletionDays(completedWithDates > 0
                ? (double) totalCompletionDays / completedWithDates
                : 0.0);

        List<PlatformOnboardingAnalyticsResponse.CompanyItem> companyItems = new ArrayList<>();
        for (Map.Entry<String, CompanyStats> entry : statsByCompanyId.entrySet()) {
            String companyId = entry.getKey();
            CompanyStats stats = entry.getValue();

            PlatformOnboardingAnalyticsResponse.CompanyItem item =
                    new PlatformOnboardingAnalyticsResponse.CompanyItem();

            item.setCompanyId(companyId);
            item.setCompanyName(companyNameById.getOrDefault(companyId, companyId));
            item.setTotalOnboardings(stats.total);
            item.setCompletedOnboardings(stats.completed);
            item.setCancelledOnboardings(stats.cancelled);
            item.setRiskOnboardings(stats.risk);
            item.setCompletionRate(stats.total > 0
                    ? (double) stats.completed / stats.total
                    : 0.0);
            item.setAverageCompletionDays(stats.completedWithDates > 0
                    ? (double) stats.totalCompletionDays / stats.completedWithDates
                    : 0.0);

            companyItems.add(item);
        }

        companyItems.sort(Comparator
                .comparing(PlatformOnboardingAnalyticsResponse.CompanyItem::getTotalOnboardings,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(PlatformOnboardingAnalyticsResponse.CompanyItem::getCompanyName,
                        Comparator.nullsLast(String::compareToIgnoreCase)));

        response.setCompanyItems(companyItems);
        return response;
    }

    private static class CompanyStats {
        private int total;
        private int completed;
        private int cancelled;
        private int risk;
        private long totalCompletionDays;
        private int completedWithDates;
    }
}