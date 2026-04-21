package com.sme.be_sme.modules.platform.facade.impl;

import com.sme.be_sme.modules.platform.api.request.*;
import com.sme.be_sme.modules.platform.api.response.*;
import com.sme.be_sme.modules.platform.facade.PlatformFacade;
import com.sme.be_sme.modules.platform.processor.analytics.*;
import com.sme.be_sme.modules.platform.processor.audit.PlatformAdminAuditLogProcessor;
import com.sme.be_sme.modules.platform.processor.company.*;
import com.sme.be_sme.modules.platform.processor.dashboard.PlatformDashboardOverviewProcessor;
import com.sme.be_sme.modules.platform.processor.dashboard.PlatformFinancialDashboardProcessor;
import com.sme.be_sme.modules.platform.processor.dashboard.PlatformRiskDashboardProcessor;
import com.sme.be_sme.modules.platform.processor.feedback.FeedbackSubmitProcessor;
import com.sme.be_sme.modules.platform.processor.feedback.PlatformFeedbackDetailProcessor;
import com.sme.be_sme.modules.platform.processor.feedback.PlatformFeedbackListProcessor;
import com.sme.be_sme.modules.platform.processor.feedback.PlatformFeedbackResolveProcessor;
import com.sme.be_sme.modules.platform.processor.monitoring.PlatformActivityLogListProcessor;
import com.sme.be_sme.modules.platform.processor.monitoring.PlatformErrorLogListProcessor;
import com.sme.be_sme.modules.platform.processor.monitoring.PlatformMonitoringMetricsProcessor;
import com.sme.be_sme.modules.platform.processor.monitoring.PlatformSystemHealthProcessor;
import com.sme.be_sme.modules.platform.processor.plan.PlatformPlanCreateProcessor;
import com.sme.be_sme.modules.platform.processor.plan.PlatformPlanDeleteProcessor;
import com.sme.be_sme.modules.platform.processor.plan.PlatformPlanListProcessor;
import com.sme.be_sme.modules.platform.processor.plan.PlatformPlanUpdateProcessor;
import com.sme.be_sme.modules.platform.processor.subscription.PlatformInvoiceListProcessor;
import com.sme.be_sme.modules.platform.processor.subscription.PlatformPaymentListProcessor;
import com.sme.be_sme.modules.platform.processor.subscription.PlatformSubscriptionDetailProcessor;
import com.sme.be_sme.modules.platform.processor.subscription.PlatformSubscriptionListProcessor;
import com.sme.be_sme.modules.platform.processor.template.PlatformCreateTemplateProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformFacadeImpl extends BaseOperationFacade implements PlatformFacade {

    private final PlatformCompanyListProcessor companyListProcessor;
    private final PlatformCompanyDetailProcessor companyDetailProcessor;
    private final PlatformCompanyActivateProcessor companyActivateProcessor;
    private final PlatformCompanyDeactivateProcessor companyDeactivateProcessor;
    private final PlatformCompanyDeleteProcessor companyDeleteProcessor;
    private final PlatformSubscriptionListProcessor subscriptionListProcessor;
    private final PlatformSubscriptionDetailProcessor subscriptionDetailProcessor;
    private final PlatformInvoiceListProcessor invoiceListProcessor;
    private final PlatformPaymentListProcessor paymentListProcessor;
    private final PlatformFinancialDashboardProcessor financialDashboardProcessor;
    private final PlatformOnboardingAnalyticsProcessor onboardingAnalyticsProcessor;
    private final PlatformPlanListProcessor planListProcessor;
    private final PlatformPlanCreateProcessor planCreateProcessor;
    private final PlatformPlanUpdateProcessor planUpdateProcessor;
    private final PlatformPlanDeleteProcessor planDeleteProcessor;
    private final PlatformSystemHealthProcessor systemHealthProcessor;
    private final PlatformErrorLogListProcessor errorLogListProcessor;
    private final PlatformActivityLogListProcessor activityLogListProcessor;
    private final PlatformFeedbackListProcessor feedbackListProcessor;
    private final PlatformFeedbackDetailProcessor feedbackDetailProcessor;
    private final PlatformFeedbackResolveProcessor feedbackResolveProcessor;
    private final FeedbackSubmitProcessor feedbackSubmitProcessor;
    private final PlatformRevenueAnalyticsProcessor revenueAnalyticsProcessor;
    private final PlatformSubscriptionAnalyticsProcessor subscriptionAnalyticsProcessor;
    private final PlatformCompanyAnalyticsProcessor companyAnalyticsProcessor;
    private final PlatformUsageAnalyticsProcessor usageAnalyticsProcessor;
    private final PlatformMonitoringMetricsProcessor monitoringMetricsProcessor;
    private final PlatformCompanySuspendProcessor companySuspendProcessor;
    private final PlatformCompanyChangePlanProcessor companyChangePlanProcessor;
    private final PlatformAdminAuditLogProcessor adminAuditLogProcessor;
    private final PlatformDashboardOverviewProcessor platformDashboardOverviewProcessor;
    private final PlatformCompanyTrendProcessor platformCompanyTrendProcessor;
    private final PlatformRevenueTrendProcessor platformRevenueTrendProcessor;
    private final PlatformPlanTrendProcessor platformPlanTrendProcessor;
    private final PlatformEmployeeAnalyticsProcessor platformEmployeeAnalyticsProcessor;
    private final PlatformEmployeeTrendProcessor platformEmployeeTrendProcessor;
    private final PlatformPlanDistributionProcessor platformPlanDistributionProcessor;
    private final PlatformForecastProcessor platformForecastProcessor;
    private final PlatformOnboardingTrendProcessor platformOnboardingTrendProcessor;
    private final PlatformRiskDashboardProcessor platformRiskDashboardProcessor;
    private final PlatformCreateTemplateProcessor platformCreateTemplateProcessor;

    @Override
    public PlatformCompanyListResponse listCompanies(PlatformCompanyListRequest request) {
        return call(companyListProcessor, request, PlatformCompanyListResponse.class);
    }

    @Override
    public PlatformCompanyDetailResponse getCompanyDetail(PlatformCompanyDetailRequest request) {
        return call(companyDetailProcessor, request, PlatformCompanyDetailResponse.class);
    }

    @Override
    public PlatformCompanyStatusResponse activateCompany(PlatformCompanyStatusRequest request) {
        return call(companyActivateProcessor, request, PlatformCompanyStatusResponse.class);
    }

    @Override
    public PlatformCompanyStatusResponse deactivateCompany(PlatformCompanyStatusRequest request) {
        return call(companyDeactivateProcessor, request, PlatformCompanyStatusResponse.class);
    }

    @Override
    public PlatformCompanyStatusResponse deleteCompany(PlatformCompanyStatusRequest request) {
        return call(companyDeleteProcessor, request, PlatformCompanyStatusResponse.class);
    }

    @Override
    public PlatformSubscriptionListResponse listSubscriptions(PlatformSubscriptionListRequest request) {
        return call(subscriptionListProcessor, request, PlatformSubscriptionListResponse.class);
    }

    @Override
    public PlatformSubscriptionDetailResponse getSubscriptionDetail(PlatformSubscriptionDetailRequest request) {
        return call(subscriptionDetailProcessor, request, PlatformSubscriptionDetailResponse.class);
    }

    @Override
    public PlatformInvoiceListResponse listInvoices(PlatformInvoiceListRequest request) {
        return call(invoiceListProcessor, request, PlatformInvoiceListResponse.class);
    }

    @Override
    public PlatformPaymentListResponse listPayments(PlatformPaymentListRequest request) {
        return call(paymentListProcessor, request, PlatformPaymentListResponse.class);
    }

    @Override
    public PlatformFinancialDashboardResponse getFinancialDashboard(PlatformFinancialDashboardRequest request) {
        return call(financialDashboardProcessor, request, PlatformFinancialDashboardResponse.class);
    }

    @Override
    public PlatformOnboardingAnalyticsResponse getOnboardingAnalytics(PlatformOnboardingAnalyticsRequest request) {
        return call(onboardingAnalyticsProcessor, request, PlatformOnboardingAnalyticsResponse.class);
    }

    @Override
    public PlatformPlanListResponse listPlans(PlatformPlanListRequest request) {
        return call(planListProcessor, request, PlatformPlanListResponse.class);
    }

    @Override
    public PlatformPlanResponse createPlan(PlatformPlanCreateRequest request) {
        return call(planCreateProcessor, request, PlatformPlanResponse.class);
    }

    @Override
    public PlatformPlanResponse updatePlan(PlatformPlanUpdateRequest request) {
        return call(planUpdateProcessor, request, PlatformPlanResponse.class);
    }

    @Override
    public PlatformPlanResponse deletePlan(PlatformPlanDeleteRequest request) {
        return call(planDeleteProcessor, request, PlatformPlanResponse.class);
    }

    @Override
    public PlatformSystemHealthResponse getSystemHealth(PlatformSystemHealthRequest request) {
        return call(systemHealthProcessor, request, PlatformSystemHealthResponse.class);
    }

    @Override
    public PlatformErrorLogListResponse listErrorLogs(PlatformErrorLogListRequest request) {
        return call(errorLogListProcessor, request, PlatformErrorLogListResponse.class);
    }

    @Override
    public PlatformActivityLogListResponse listActivityLogs(PlatformActivityLogListRequest request) {
        return call(activityLogListProcessor, request, PlatformActivityLogListResponse.class);
    }

    @Override
    public PlatformFeedbackListResponse listFeedback(PlatformFeedbackListRequest request) {
        return call(feedbackListProcessor, request, PlatformFeedbackListResponse.class);
    }

    @Override
    public PlatformFeedbackDetailResponse getFeedbackDetail(PlatformFeedbackDetailRequest request) {
        return call(feedbackDetailProcessor, request, PlatformFeedbackDetailResponse.class);
    }

    @Override
    public PlatformFeedbackResolveResponse resolveFeedback(PlatformFeedbackResolveRequest request) {
        return call(feedbackResolveProcessor, request, PlatformFeedbackResolveResponse.class);
    }

    @Override
    public FeedbackSubmitResponse submitFeedback(FeedbackSubmitRequest request) {
        return call(feedbackSubmitProcessor, request, FeedbackSubmitResponse.class);
    }

    @Override
    public PlatformRevenueAnalyticsResponse getRevenueAnalytics(PlatformRevenueAnalyticsRequest request) {
        return call(revenueAnalyticsProcessor, request, PlatformRevenueAnalyticsResponse.class);
    }

    @Override
    public PlatformSubscriptionAnalyticsResponse getSubscriptionAnalytics(PlatformSubscriptionAnalyticsRequest request) {
        return call(subscriptionAnalyticsProcessor, request, PlatformSubscriptionAnalyticsResponse.class);
    }

    @Override
    public PlatformCompanyAnalyticsResponse getCompanyAnalytics(PlatformCompanyAnalyticsRequest request) {
        return call(companyAnalyticsProcessor, request, PlatformCompanyAnalyticsResponse.class);
    }

    @Override
    public PlatformUsageAnalyticsResponse getUsageAnalytics(PlatformUsageAnalyticsRequest request) {
        return call(usageAnalyticsProcessor, request, PlatformUsageAnalyticsResponse.class);
    }

    @Override
    public PlatformMonitoringMetricsResponse getMonitoringMetrics(PlatformMonitoringMetricsRequest request) {
        return call(monitoringMetricsProcessor, request, PlatformMonitoringMetricsResponse.class);
    }

    @Override
    public PlatformCompanySuspendResponse suspendCompany(PlatformCompanySuspendRequest request) {
        return call(companySuspendProcessor, request, PlatformCompanySuspendResponse.class);
    }

    @Override
    public PlatformCompanyChangePlanResponse changeCompanyPlan(PlatformCompanyChangePlanRequest request) {
        return call(companyChangePlanProcessor, request, PlatformCompanyChangePlanResponse.class);
    }

    @Override
    public PlatformAdminAuditLogResponse getAdminAuditLog(PlatformAdminAuditLogRequest request) {
        return call(adminAuditLogProcessor, request, PlatformAdminAuditLogResponse.class);
    }

    @Override
    public PlatformDashboardOverviewResponse getDashboardOverview(PlatformDashboardOverviewRequest request) {
        return call(platformDashboardOverviewProcessor, request, PlatformDashboardOverviewResponse.class);
    }

    @Override
    public PlatformCompanyTrendResponse getCompanyTrend(PlatformCompanyTrendRequest request) {
        return call(platformCompanyTrendProcessor, request, PlatformCompanyTrendResponse.class);
    }

    @Override
    public PlatformRevenueTrendResponse getRevenueTrend(PlatformRevenueTrendRequest request) {
        return call(platformRevenueTrendProcessor, request, PlatformRevenueTrendResponse.class);
    }

    @Override
    public PlatformPlanTrendResponse getPlanTrend(PlatformPlanTrendRequest request) {
        return call(platformPlanTrendProcessor, request, PlatformPlanTrendResponse.class);
    }

    @Override
    public PlatformEmployeeAnalyticsResponse getEmployeeAnalytics(PlatformEmployeeAnalyticsRequest request) {
        return call(platformEmployeeAnalyticsProcessor, request, PlatformEmployeeAnalyticsResponse.class);
    }

    @Override
    public PlatformEmployeeTrendResponse getEmployeeTrend(PlatformEmployeeTrendRequest request) {
        return call(platformEmployeeTrendProcessor, request, PlatformEmployeeTrendResponse.class);
    }

    @Override
    public PlatformPlanDistributionResponse getPlanDistribution(PlatformPlanDistributionRequest request) {
        return call(platformPlanDistributionProcessor, request, PlatformPlanDistributionResponse.class);
    }

    @Override
    public PlatformForecastResponse getForecast(PlatformForecastRequest request) {
        return call(platformForecastProcessor, request, PlatformForecastResponse.class);
    }

    @Override
    public PlatformOnboardingTrendResponse getOnboardingTrend(PlatformOnboardingTrendRequest request) {
        return call(platformOnboardingTrendProcessor, request, PlatformOnboardingTrendResponse.class);
    }

    @Override
    public PlatformRiskDashboardResponse getRiskDashboard(PlatformRiskDashboardRequest request) {
        return call(platformRiskDashboardProcessor, request, PlatformRiskDashboardResponse.class);
    }

    @Override
    public CreatePlatformTemplateResponse createPlatformTemplate(CreatePlatformTemplateRequest request) {
        return call(platformCreateTemplateProcessor, request, CreatePlatformTemplateResponse.class);
    }
}
