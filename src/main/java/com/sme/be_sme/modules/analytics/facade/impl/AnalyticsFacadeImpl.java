package com.sme.be_sme.modules.analytics.facade.impl;

import com.sme.be_sme.modules.analytics.api.request.*;
import com.sme.be_sme.modules.analytics.api.response.*;
import com.sme.be_sme.modules.analytics.facade.AnalyticsFacade;
import com.sme.be_sme.modules.analytics.processor.*;
import com.sme.be_sme.modules.onboarding.support.OnboardingInstanceStatus;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalyticsFacadeImpl extends BaseOperationFacade implements AnalyticsFacade {

    private final CompanyOnboardingSummaryProcessor companyOnboardingSummaryProcessor;
    private final CompanyOnboardingFunnelProcessor companyOnboardingFunnelProcessor;
    private final CompanyOnboardingByDepartmentProcessor companyOnboardingByDepartmentProcessor;
    private final CompanyTaskCompletionProcessor companyTaskCompletionProcessor;
    private final CompanyOnboardingTrendProcessor companyOnboardingTrendProcessor;
    private final CompanyOnboardingTemplateScoreboardProcessor companyOnboardingTemplateScoreboardProcessor;
    private final PlatformSubscriptionMetricsProcessor platformSubscriptionMetricsProcessor;
    private final ManagerTeamSummaryProcessor managerTeamSummaryProcessor;
    private final CandidateFitAssessProcessor candidateFitAssessProcessor;

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
    public CompanyOnboardingTrendResponse getCompanyOnboardingTrend(CompanyOnboardingTrendRequest request) {
        return call(companyOnboardingTrendProcessor, request, CompanyOnboardingTrendResponse.class);
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
    public CompanyOnboardingTemplateScoreboardResponse getCompletedOnboardingTemplateScoreboard(
            CompanyOnboardingTemplateScoreboardRequest request) {
        CompanyOnboardingTemplateScoreboardRequest effectiveRequest =
                Objects.requireNonNullElseGet(request, CompanyOnboardingTemplateScoreboardRequest::new);
        effectiveRequest.setStatus(OnboardingInstanceStatus.DONE);
        return call(
                companyOnboardingTemplateScoreboardProcessor,
                effectiveRequest,
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

    @Override
    public CandidateFitAssessResponse assessCandidateFit(CandidateFitAssessRequest request) {
        return call(candidateFitAssessProcessor, request, CandidateFitAssessResponse.class);
    }
}
