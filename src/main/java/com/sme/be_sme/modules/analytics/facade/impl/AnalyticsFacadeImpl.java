package com.sme.be_sme.modules.analytics.facade.impl;

import com.sme.be_sme.modules.analytics.api.request.*;
import com.sme.be_sme.modules.analytics.api.response.*;
import com.sme.be_sme.modules.analytics.facade.AnalyticsFacade;
import com.sme.be_sme.modules.analytics.processor.*;
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
    private final CompanyOnboardingTemplateScoreboardProcessor companyOnboardingTemplateScoreboardProcessor;
    private final PlatformSubscriptionMetricsProcessor platformSubscriptionMetricsProcessor;
    private final ManagerTeamSummaryProcessor managerTeamSummaryProcessor;

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
    public CompanyOnboardingTemplateScoreboardResponse getOnboardingTemplateScoreboard(
            CompanyOnboardingTemplateScoreboardRequest request) {
        return call(
                companyOnboardingTemplateScoreboardProcessor,
                request,
                CompanyOnboardingTemplateScoreboardResponse.class);
    }

    @Override
    public PlatformSubscriptionMetricsResponse getPlatformSubscriptionMetrics(PlatformSubscriptionMetricsRequest request) {
        return call(platformSubscriptionMetricsProcessor, request, PlatformSubscriptionMetricsResponse.class);
    }

    @Override
    public ManagerTeamSummaryResponse getManagerTeamSummary(ManagerTeamSummaryRequest request) {
        return call(managerTeamSummaryProcessor, request, ManagerTeamSummaryResponse.class);
    }
}
