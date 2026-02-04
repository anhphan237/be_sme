package com.sme.be_sme.modules.analytics.facade.impl;

import com.sme.be_sme.modules.analytics.api.request.CompanyOnboardingSummaryRequest;
import com.sme.be_sme.modules.analytics.api.request.PlatformSubscriptionMetricsRequest;
import com.sme.be_sme.modules.analytics.api.response.CompanyOnboardingSummaryResponse;
import com.sme.be_sme.modules.analytics.api.response.PlatformSubscriptionMetricsResponse;
import com.sme.be_sme.modules.analytics.facade.AnalyticsFacade;
import com.sme.be_sme.modules.analytics.processor.CompanyOnboardingSummaryProcessor;
import com.sme.be_sme.modules.analytics.processor.PlatformSubscriptionMetricsProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalyticsFacadeImpl extends BaseOperationFacade implements AnalyticsFacade {

    private final CompanyOnboardingSummaryProcessor companyOnboardingSummaryProcessor;
    private final PlatformSubscriptionMetricsProcessor platformSubscriptionMetricsProcessor;

    @Override
    public CompanyOnboardingSummaryResponse getCompanyOnboardingSummary(CompanyOnboardingSummaryRequest request) {
        return call(companyOnboardingSummaryProcessor, request, CompanyOnboardingSummaryResponse.class);
    }

    @Override
    public PlatformSubscriptionMetricsResponse getPlatformSubscriptionMetrics(PlatformSubscriptionMetricsRequest request) {
        return call(platformSubscriptionMetricsProcessor, request, PlatformSubscriptionMetricsResponse.class);
    }
}
