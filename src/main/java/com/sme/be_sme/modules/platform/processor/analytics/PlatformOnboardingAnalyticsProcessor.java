package com.sme.be_sme.modules.platform.processor.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformOnboardingAnalyticsRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformOnboardingAnalyticsResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformOnboardingAnalyticsProcessor extends BaseBizProcessor<BizContext> {

    private static final String STATUS_COMPLETED = "COMPLETED";

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformOnboardingAnalyticsRequest request = objectMapper.convertValue(payload, PlatformOnboardingAnalyticsRequest.class);

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
        response.setCompletionRate(totalOnboardings > 0 ? (double) completedOnboardings / totalOnboardings : 0.0);
        response.setAverageCompletionDays(completedWithDates > 0 ? (double) totalCompletionDays / completedWithDates : 0.0);
        return response;
    }
}
