package com.sme.be_sme.modules.analytics.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.analytics.api.request.PlatformSubscriptionMetricsRequest;
import com.sme.be_sme.modules.analytics.api.response.PlatformSubscriptionMetricsResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformSubscriptionMetricsProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        objectMapper.convertValue(payload, PlatformSubscriptionMetricsRequest.class);
        PlatformSubscriptionMetricsResponse response = new PlatformSubscriptionMetricsResponse();
        response.setActiveSubscriptions(0);
        response.setMonthlyRecurringRevenue(0.0);
        return response;
    }
}
