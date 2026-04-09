package com.sme.be_sme.modules.platform.processor.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformSubscriptionAnalyticsRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformSubscriptionAnalyticsResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformSubscriptionAnalyticsProcessor extends BaseBizProcessor<BizContext> {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_SUSPENDED = "SUSPENDED";

    private final ObjectMapper objectMapper;
    private final SubscriptionMapper subscriptionMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformSubscriptionAnalyticsRequest request = objectMapper.convertValue(payload, PlatformSubscriptionAnalyticsRequest.class);

        Date startDate = PlatformAnalyticsSupport.parseDate(request.getStartDate(), true);
        Date endDate = PlatformAnalyticsSupport.parseDate(request.getEndDate(), false);
        List<SubscriptionEntity> allSubs = subscriptionMapper.selectAll();

        int totalSubscriptions = 0;
        int activeSubscriptions = 0;
        int newSubscriptions = 0;
        int cancelledSubscriptions = 0;
        int suspendedSubscriptions = 0;
        int activeAtStart = 0;
        int churnedInRange = 0;

        for (SubscriptionEntity sub : allSubs) {
            if (sub == null) {
                continue;
            }
            totalSubscriptions++;
            if (STATUS_ACTIVE.equalsIgnoreCase(sub.getStatus())) {
                activeSubscriptions++;
            }
            if (STATUS_CANCELLED.equalsIgnoreCase(sub.getStatus())) {
                cancelledSubscriptions++;
            }
            if (STATUS_SUSPENDED.equalsIgnoreCase(sub.getStatus())) {
                suspendedSubscriptions++;
            }
            if (PlatformAnalyticsSupport.inRange(sub.getCreatedAt(), startDate, endDate)) {
                newSubscriptions++;
            }

            boolean churned = (STATUS_CANCELLED.equalsIgnoreCase(sub.getStatus()) || STATUS_SUSPENDED.equalsIgnoreCase(sub.getStatus()))
                    && PlatformAnalyticsSupport.inRange(sub.getUpdatedAt(), startDate, endDate);
            if (churned) {
                churnedInRange++;
            }
            boolean wasActiveAtStart = startDate == null
                    ? STATUS_ACTIVE.equalsIgnoreCase(sub.getStatus()) || STATUS_CANCELLED.equalsIgnoreCase(sub.getStatus()) || STATUS_SUSPENDED.equalsIgnoreCase(sub.getStatus())
                    : sub.getCreatedAt() != null && sub.getCreatedAt().before(startDate)
                    && (STATUS_ACTIVE.equalsIgnoreCase(sub.getStatus()) || churned);
            if (wasActiveAtStart) {
                activeAtStart++;
            }
        }

        PlatformSubscriptionAnalyticsResponse response = new PlatformSubscriptionAnalyticsResponse();
        response.setTotalSubscriptions(totalSubscriptions);
        response.setActiveSubscriptions(activeSubscriptions);
        response.setNewSubscriptions(newSubscriptions);
        response.setCancelledSubscriptions(cancelledSubscriptions);
        response.setSuspendedSubscriptions(suspendedSubscriptions);
        response.setChurnRate(activeAtStart > 0 ? (double) churnedInRange / activeAtStart : 0.0);
        return response;
    }
}
