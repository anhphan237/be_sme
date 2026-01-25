package com.sme.be_sme.modules.analytics.facade.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.modules.analytics.facade.AnalyticsFacade;
import com.sme.be_sme.shared.gateway.api.OperationStubResponse;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsFacadeImpl implements AnalyticsFacade {

    @Override
    public OperationStubResponse getCompanyOnboardingSummary(JsonNode payload) {
        return OperationStubResponse.notImplemented("com.sme.analytics.company.onboarding.summary");
    }

    @Override
    public OperationStubResponse getPlatformSubscriptionMetrics(JsonNode payload) {
        return OperationStubResponse.notImplemented("com.sme.analytics.platform.subscription.metrics");
    }
}
