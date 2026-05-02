package com.sme.be_sme.modules.analytics.facade;

import com.sme.be_sme.modules.analytics.api.request.*;
import com.sme.be_sme.modules.analytics.api.response.*;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface AnalyticsFacade extends OperationFacadeProvider {

    @OperationType("com.sme.analytics.company.onboarding.summary")
    CompanyOnboardingSummaryResponse getCompanyOnboardingSummary(CompanyOnboardingSummaryRequest request);

    @OperationType("com.sme.analytics.company.onboarding.funnel")
    CompanyOnboardingFunnelResponse getCompanyOnboardingFunnel(CompanyOnboardingFunnelRequest request);

    @OperationType("com.sme.analytics.company.onboarding.byDepartment")
    CompanyOnboardingByDepartmentResponse getCompanyOnboardingByDepartment(CompanyOnboardingByDepartmentRequest request);

    @OperationType("com.sme.analytics.company.task.completion")
    CompanyTaskCompletionResponse getCompanyTaskCompletion(CompanyTaskCompletionRequest request);

    @OperationType("com.sme.analytics.company.onboarding.trend")
    CompanyOnboardingTrendResponse getCompanyOnboardingTrend(CompanyOnboardingTrendRequest request);

    @OperationType("com.sme.analytics.onboarding.template.scoreboard")
    CompanyOnboardingTemplateScoreboardResponse getOnboardingTemplateScoreboard(
            CompanyOnboardingTemplateScoreboardRequest request);

    @OperationType("com.sme.analytics.onboarding.template.scoreboard.completed")
    CompanyOnboardingTemplateScoreboardResponse getCompletedOnboardingTemplateScoreboard(
            CompanyOnboardingTemplateScoreboardRequest request);

    @OperationType("com.sme.analytics.platform.subscription.metrics")
    PlatformSubscriptionMetricsResponse getPlatformSubscriptionMetrics(PlatformSubscriptionMetricsRequest request);

    @OperationType("com.sme.analytics.manager.team.summary")
    ManagerTeamSummaryResponse getManagerTeamSummary(ManagerTeamSummaryRequest request);

    @OperationType("com.sme.analytics.candidate.fit.assess")
    CandidateFitAssessResponse assessCandidateFit(CandidateFitAssessRequest request);
}
