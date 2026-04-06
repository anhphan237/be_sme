package com.sme.be_sme.modules.platform.processor.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.billing.infrastructure.mapper.SubscriptionMapper;
import com.sme.be_sme.modules.billing.infrastructure.persistence.entity.SubscriptionEntity;
import com.sme.be_sme.modules.platform.api.request.PlatformSubscriptionAnalyticsRequest;
import com.sme.be_sme.modules.platform.api.response.PlatformSubscriptionAnalyticsResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
        PlatformSubscriptionAnalyticsRequest request =
                objectMapper.convertValue(payload, PlatformSubscriptionAnalyticsRequest.class);

        Date startDate = parseDate(request.getStartDate(), true);
        Date endDate = parseDate(request.getEndDate(), false);

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

            String status = sub.getStatus();

            if (STATUS_ACTIVE.equalsIgnoreCase(status)) {
                activeSubscriptions++;
            }
            if (STATUS_CANCELLED.equalsIgnoreCase(status)) {
                cancelledSubscriptions++;
            }
            if (STATUS_SUSPENDED.equalsIgnoreCase(status)) {
                suspendedSubscriptions++;
            }

            if (inRange(sub.getCreatedAt(), startDate, endDate)) {
                newSubscriptions++;
            }

            boolean isChurnedInRange =
                    (STATUS_CANCELLED.equalsIgnoreCase(status) || STATUS_SUSPENDED.equalsIgnoreCase(status))
                            && inRange(sub.getUpdatedAt(), startDate, endDate);

            if (isChurnedInRange) {
                churnedInRange++;
            }

            boolean wasActiveAtStart =
                    sub.getCreatedAt() != null
                            && startDate != null
                            && sub.getCreatedAt().before(startDate)
                            && (STATUS_ACTIVE.equalsIgnoreCase(status) || isChurnedInRange);

            if (startDate == null) {
                wasActiveAtStart = STATUS_ACTIVE.equalsIgnoreCase(status)
                        || STATUS_CANCELLED.equalsIgnoreCase(status)
                        || STATUS_SUSPENDED.equalsIgnoreCase(status);
            }

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
        response.setChurnRate(activeAtStart > 0 ? (double) churnedInRange / activeAtStart : null);
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
}