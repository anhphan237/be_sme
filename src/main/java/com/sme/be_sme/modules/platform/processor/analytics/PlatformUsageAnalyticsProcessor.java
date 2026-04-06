package com.sme.be_sme.modules.platform.processor.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformUsageAnalyticsRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformUsageAnalyticsResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformUsageAnalyticsProcessor extends BaseBizProcessor<BizContext> {

    private static final String STATUS_COMPLETED = "COMPLETED";

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformUsageAnalyticsRequest request =
                objectMapper.convertValue(payload, PlatformUsageAnalyticsRequest.class);

        Date startDate = parseDate(request.getStartDate(), true);
        Date endDate = parseDate(request.getEndDate(), false);

        List<OnboardingInstanceEntity> allInstances = onboardingInstanceMapper.selectAll();

        int totalOnboardings = 0;
        int totalCompletedOnboardings = 0;
        Set<String> companyIds = new HashSet<>();

        for (OnboardingInstanceEntity instance : allInstances) {
            if (instance == null) {
                continue;
            }
            if (!inRange(instance.getCreatedAt(), startDate, endDate)) {
                continue;
            }

            totalOnboardings++;

            if (STATUS_COMPLETED.equalsIgnoreCase(instance.getStatus())) {
                totalCompletedOnboardings++;
            }

            String companyId = extractCompanyId(instance);
            if (StringUtils.hasText(companyId)) {
                companyIds.add(companyId);
            }
        }

        PlatformUsageAnalyticsResponse response = new PlatformUsageAnalyticsResponse();
        response.setTotalOnboardings(totalOnboardings);
        response.setTotalCompletedOnboardings(totalCompletedOnboardings);

        // Chưa wire mapper survey / feedback trong phần code bạn gửi,
        // nên giữ 0 để compile-safe. Nếu bạn có đúng tên mapper/entity,
        // mình nối thêm cho bạn 2 field này sau.
        response.setTotalSurveyResponses(0);
        response.setTotalFeedbacks(0);

        response.setAvgOnboardingsPerCompany(
                !companyIds.isEmpty() ? (double) totalOnboardings / companyIds.size() : null
        );
        return response;
    }

    private boolean inRange(Date value, Date start, Date end) {
        if (value == null) {
            return false;
        }
        if (start != null && value.before(start)) {
            return false;
        }
        if (end != null && !value.before(end)) {
            return false;
        }
        return true;
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

    private String extractCompanyId(OnboardingInstanceEntity instance) {
        try {
            Method getter = instance.getClass().getMethod("getCompanyId");
            Object value = getter.invoke(instance);
            return value != null ? String.valueOf(value) : null;
        } catch (Exception ignored) {
        }

        try {
            Field field = instance.getClass().getDeclaredField("companyId");
            field.setAccessible(true);
            Object value = field.get(instance);
            return value != null ? String.valueOf(value) : null;
        } catch (Exception ignored) {
        }

        return null;
    }
}