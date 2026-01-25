package com.sme.be_sme.modules.analytics.facade;

import com.sme.be_sme.modules.analytics.api.request.CompanyOnboardingSummaryRequest;
import com.sme.be_sme.modules.analytics.api.request.PlatformSubscriptionMetricsRequest;
import com.sme.be_sme.modules.analytics.api.response.CompanyOnboardingSummaryResponse;
import com.sme.be_sme.modules.analytics.api.response.PlatformSubscriptionMetricsResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface AnalyticsFacade extends OperationFacadeProvider {

    @OperationType("com.sme.analytics.company.onboarding.summary")
    CompanyOnboardingSummaryResponse getCompanyOnboardingSummary(CompanyOnboardingSummaryRequest request);

    @OperationType("com.sme.analytics.platform.subscription.metrics")
    PlatformSubscriptionMetricsResponse getPlatformSubscriptionMetrics(PlatformSubscriptionMetricsRequest request);
}
