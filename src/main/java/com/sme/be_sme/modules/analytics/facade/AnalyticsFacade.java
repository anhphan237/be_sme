package com.sme.be_sme.modules.analytics.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.api.OperationStubResponse;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface AnalyticsFacade extends OperationFacadeProvider {

    @OperationType("com.sme.analytics.company.onboarding.summary")
    OperationStubResponse getCompanyOnboardingSummary(JsonNode payload);

    @OperationType("com.sme.analytics.platform.subscription.metrics")
    OperationStubResponse getPlatformSubscriptionMetrics(JsonNode payload);
}
