package com.sme.be_sme.modules.analytics.facade.impl;

import com.sme.be_sme.modules.analytics.api.request.CompanyOnboardingByDepartmentRequest;
import com.sme.be_sme.modules.analytics.api.request.CompanyOnboardingFunnelRequest;
import com.sme.be_sme.modules.analytics.api.request.CompanyOnboardingSummaryRequest;
import com.sme.be_sme.modules.analytics.api.request.CompanyTaskCompletionRequest;
import com.sme.be_sme.modules.analytics.api.request.PlatformSubscriptionMetricsRequest;
import com.sme.be_sme.modules.analytics.api.response.CompanyOnboardingByDepartmentResponse;
import com.sme.be_sme.modules.analytics.api.response.CompanyOnboardingFunnelResponse;
import com.sme.be_sme.modules.analytics.api.response.CompanyOnboardingSummaryResponse;
import com.sme.be_sme.modules.analytics.api.response.CompanyTaskCompletionResponse;
import com.sme.be_sme.modules.analytics.api.response.PlatformSubscriptionMetricsResponse;
import com.sme.be_sme.modules.analytics.facade.AnalyticsFacade;
import com.sme.be_sme.modules.analytics.processor.CompanyOnboardingByDepartmentProcessor;
import com.sme.be_sme.modules.analytics.processor.CompanyOnboardingFunnelProcessor;
import com.sme.be_sme.modules.analytics.processor.CompanyOnboardingSummaryProcessor;
import com.sme.be_sme.modules.analytics.processor.CompanyTaskCompletionProcessor;
import com.sme.be_sme.modules.analytics.processor.PlatformSubscriptionMetricsProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalyticsFacadeImpl extends BaseOperationFacade implements AnalyticsFacade {

    private final CompanyOnboardingSummaryProcessor companyOnboardingSummaryProcessor;
    private final CompanyOnboardingFunnelProcessor companyOnboardingFunnelProcessor;
    private final CompanyOnboardingByDepartmentProcessor companyOnboardingByDepartmentProcessor;
    private final CompanyTaskCompletionProcessor companyTaskCompletionProcessor;
    private final PlatformSubscriptionMetricsProcessor platformSubscriptionMetricsProcessor;

    @Override
    public CompanyOnboardingSummaryResponse getCompanyOnboardingSummary(CompanyOnboardingSummaryRequest request) {
        return call(companyOnboardingSummaryProcessor, request, CompanyOnboardingSummaryResponse.class);
    }

    @Override
    public CompanyOnboardingFunnelResponse getCompanyOnboardingFunnel(CompanyOnboardingFunnelRequest request) {
        return call(companyOnboardingFunnelProcessor, request, CompanyOnboardingFunnelResponse.class);
    }

    @Override
    public CompanyOnboardingByDepartmentResponse getCompanyOnboardingByDepartment(CompanyOnboardingByDepartmentRequest request) {
        return call(companyOnboardingByDepartmentProcessor, request, CompanyOnboardingByDepartmentResponse.class);
    }

    @Override
    public CompanyTaskCompletionResponse getCompanyTaskCompletion(CompanyTaskCompletionRequest request) {
        return call(companyTaskCompletionProcessor, request, CompanyTaskCompletionResponse.class);
    }

    @Override
    public PlatformSubscriptionMetricsResponse getPlatformSubscriptionMetrics(PlatformSubscriptionMetricsRequest request) {
        return call(platformSubscriptionMetricsProcessor, request, PlatformSubscriptionMetricsResponse.class);
    }
}
