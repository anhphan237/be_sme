package com.sme.be_sme.modules.platform.processor.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformOnboardingAnalyticsRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformOnboardingAnalyticsResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformOnboardingAnalyticsProcessor extends BaseBizProcessor<BizContext> {

    private static final String STATUS_COMPLETED = "COMPLETED";

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformOnboardingAnalyticsRequest request = objectMapper.convertValue(payload, PlatformOnboardingAnalyticsRequest.class);

        Date startDate = parseDate(request.getStartDate(), true);
        Date endDate = parseDate(request.getEndDate(), false);

        List<OnboardingInstanceEntity> allInstances = onboardingInstanceMapper.selectAll();

        List<OnboardingInstanceEntity> filtered = new ArrayList<>();
        for (OnboardingInstanceEntity instance : allInstances) {
            if (instance == null) continue;
            if (startDate != null && instance.getCreatedAt() != null
                    && instance.getCreatedAt().before(startDate)) {
                continue;
            }
            if (endDate != null && instance.getCreatedAt() != null
                    && instance.getCreatedAt().after(endDate)) {
                continue;
            }
            filtered.add(instance);
        }

        int totalOnboardings = filtered.size();
        int completedOnboardings = 0;
        long totalCompletionDays = 0;
        int completedWithDates = 0;

        for (OnboardingInstanceEntity instance : filtered) {
            if (STATUS_COMPLETED.equalsIgnoreCase(instance.getStatus())) {
                completedOnboardings++;
                if (instance.getCompletedAt() != null && instance.getCreatedAt() != null) {
                    long diffMs = instance.getCompletedAt().getTime() - instance.getCreatedAt().getTime();
                    totalCompletionDays += TimeUnit.MILLISECONDS.toDays(diffMs);
                    completedWithDates++;
                }
            }
        }

        PlatformOnboardingAnalyticsResponse response = new PlatformOnboardingAnalyticsResponse();
        response.setTotalOnboardings(totalOnboardings);
        response.setCompletedOnboardings(completedOnboardings);
        response.setCompletionRate(totalOnboardings > 0
                ? (double) completedOnboardings / totalOnboardings
                : null);
        response.setAverageCompletionDays(completedWithDates > 0
                ? (double) totalCompletionDays / completedWithDates
                : null);
        return response;
    }

    private Date parseDate(String isoDate, boolean startOfDay) {
        if (!StringUtils.hasText(isoDate)) {
            return null;
        }
        LocalDate ld = LocalDate.parse(isoDate);
        if (startOfDay) {
            return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        return Date.from(ld.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
