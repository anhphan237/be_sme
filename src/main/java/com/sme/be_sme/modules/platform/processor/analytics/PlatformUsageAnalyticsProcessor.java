package com.sme.be_sme.modules.platform.processor.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformUsageAnalyticsRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformUsageAnalyticsResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformUsageAnalyticsProcessor extends BaseBizProcessor<BizContext> {

    private static final String STATUS_COMPLETED = "COMPLETED";

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final UserMapper userMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformUsageAnalyticsRequest request = objectMapper.convertValue(payload, PlatformUsageAnalyticsRequest.class);

        Date startDate = PlatformAnalyticsSupport.parseDate(request.getStartDate(), true);
        Date endDate = PlatformAnalyticsSupport.parseDate(request.getEndDate(), false);
        List<OnboardingInstanceEntity> allInstances = onboardingInstanceMapper.selectAll();
        List<UserEntity> allUsers = userMapper.selectAll();

        int totalOnboardings = 0;
        int totalCompletedOnboardings = 0;
        int totalEmployees = 0;
        Set<String> companyIds = new HashSet<>();

        for (OnboardingInstanceEntity instance : allInstances) {
            if (instance == null || !PlatformAnalyticsSupport.inRange(instance.getCreatedAt(), startDate, endDate)) {
                continue;
            }
            totalOnboardings++;
            if (STATUS_COMPLETED.equalsIgnoreCase(instance.getStatus())) {
                totalCompletedOnboardings++;
            }
            String companyId = PlatformAnalyticsSupport.companyId(instance);
            if (companyId != null) {
                companyIds.add(companyId);
            }
        }

        for (UserEntity user : allUsers) {
            if (user != null && PlatformAnalyticsSupport.isEmployee(user)) {
                totalEmployees++;
                if (user.getCompanyId() != null) {
                    companyIds.add(user.getCompanyId());
                }
            }
        }

        PlatformUsageAnalyticsResponse response = new PlatformUsageAnalyticsResponse();
        response.setTotalOnboardings(totalOnboardings);
        response.setTotalCompletedOnboardings(totalCompletedOnboardings);
        response.setTotalSurveyResponses(0);
        response.setTotalFeedbacks(0);
        response.setAvgOnboardingsPerCompany(!companyIds.isEmpty() ? (double) totalOnboardings / companyIds.size() : 0.0);
        return response;
    }
}
