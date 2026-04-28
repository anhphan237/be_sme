package com.sme.be_sme.modules.platform.facade;

import com.sme.be_sme.modules.platform.api.request.*;
import com.sme.be_sme.modules.platform.api.response.*;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface PlatformFacade extends OperationFacadeProvider {

    @OperationType("com.sme.platform.company.list")
    PlatformCompanyListResponse listCompanies(PlatformCompanyListRequest request);

    @OperationType("com.sme.platform.company.detail")
    PlatformCompanyDetailResponse getCompanyDetail(PlatformCompanyDetailRequest request);

    @OperationType("com.sme.platform.company.activate")
    PlatformCompanyStatusResponse activateCompany(PlatformCompanyStatusRequest request);

    @OperationType("com.sme.platform.company.deactivate")
    PlatformCompanyStatusResponse deactivateCompany(PlatformCompanyStatusRequest request);

    @OperationType("com.sme.platform.company.delete")
    PlatformCompanyStatusResponse deleteCompany(PlatformCompanyStatusRequest request);

    @OperationType("com.sme.platform.subscription.list")
    PlatformSubscriptionListResponse listSubscriptions(PlatformSubscriptionListRequest request);

    @OperationType("com.sme.platform.subscription.detail")
    PlatformSubscriptionDetailResponse getSubscriptionDetail(PlatformSubscriptionDetailRequest request);

    @OperationType("com.sme.platform.invoice.list")
    PlatformInvoiceListResponse listInvoices(PlatformInvoiceListRequest request);

    @OperationType("com.sme.platform.payment.list")
    PlatformPaymentListResponse listPayments(PlatformPaymentListRequest request);

    @OperationType("com.sme.platform.dashboard.financial")
    PlatformFinancialDashboardResponse getFinancialDashboard(PlatformFinancialDashboardRequest request);

    @OperationType("com.sme.platform.analytics.onboarding")
    PlatformOnboardingAnalyticsResponse getOnboardingAnalytics(PlatformOnboardingAnalyticsRequest request);

    @OperationType("com.sme.platform.plan.list")
    PlatformPlanListResponse listPlans(PlatformPlanListRequest request);

    @OperationType("com.sme.platform.plan.create")
    PlatformPlanResponse createPlan(PlatformPlanCreateRequest request);

    @OperationType("com.sme.platform.plan.update")
    PlatformPlanResponse updatePlan(PlatformPlanUpdateRequest request);

    @OperationType("com.sme.platform.plan.delete")
    PlatformPlanResponse deletePlan(PlatformPlanDeleteRequest request);

    @OperationType("com.sme.platform.system.health")
    PlatformSystemHealthResponse getSystemHealth(PlatformSystemHealthRequest request);

    @OperationType("com.sme.platform.system.errorLog")
    PlatformErrorLogListResponse listErrorLogs(PlatformErrorLogListRequest request);

    @OperationType("com.sme.platform.system.activityLog")
    PlatformActivityLogListResponse listActivityLogs(PlatformActivityLogListRequest request);

    @OperationType("com.sme.platform.feedback.list")
    PlatformFeedbackListResponse listFeedback(PlatformFeedbackListRequest request);

    @OperationType("com.sme.platform.feedback.detail")
    PlatformFeedbackDetailResponse getFeedbackDetail(PlatformFeedbackDetailRequest request);

    @OperationType("com.sme.platform.feedback.resolve")
    PlatformFeedbackResolveResponse resolveFeedback(PlatformFeedbackResolveRequest request);

    @OperationType("com.sme.feedback.submit")
    FeedbackSubmitResponse submitFeedback(FeedbackSubmitRequest request);

    @OperationType("com.sme.platform.analytics.revenue")
    PlatformRevenueAnalyticsResponse getRevenueAnalytics(PlatformRevenueAnalyticsRequest request);

    @OperationType("com.sme.platform.analytics.subscription")
    PlatformSubscriptionAnalyticsResponse getSubscriptionAnalytics(PlatformSubscriptionAnalyticsRequest request);

    @OperationType("com.sme.platform.analytics.company")
    PlatformCompanyAnalyticsResponse getCompanyAnalytics(PlatformCompanyAnalyticsRequest request);

    @OperationType("com.sme.platform.analytics.usage")
    PlatformUsageAnalyticsResponse getUsageAnalytics(PlatformUsageAnalyticsRequest request);

    @OperationType("com.sme.platform.monitoring.metrics")
    PlatformMonitoringMetricsResponse getMonitoringMetrics(PlatformMonitoringMetricsRequest request);

    @OperationType("com.sme.platform.company.suspend")
    PlatformCompanySuspendResponse suspendCompany(PlatformCompanySuspendRequest request);

    @OperationType("com.sme.platform.company.changePlan")
    PlatformCompanyChangePlanResponse changeCompanyPlan(PlatformCompanyChangePlanRequest request);

    @OperationType("com.sme.platform.audit.adminLog")
    PlatformAdminAuditLogResponse getAdminAuditLog(PlatformAdminAuditLogRequest request);

    @OperationType("com.sme.platform.dashboard.overview")
    PlatformDashboardOverviewResponse getDashboardOverview(PlatformDashboardOverviewRequest request);

    @OperationType("com.sme.platform.analytics.company.trend")
    PlatformCompanyTrendResponse getCompanyTrend(PlatformCompanyTrendRequest request);

    @OperationType("com.sme.platform.analytics.revenue.trend")
    PlatformRevenueTrendResponse getRevenueTrend(PlatformRevenueTrendRequest request);

    @OperationType("com.sme.platform.analytics.plan.trend")
    PlatformPlanTrendResponse getPlanTrend(PlatformPlanTrendRequest request);

    @OperationType("com.sme.platform.analytics.employee")
    PlatformEmployeeAnalyticsResponse getEmployeeAnalytics(PlatformEmployeeAnalyticsRequest request);

    @OperationType("com.sme.platform.analytics.employee.trend")
    PlatformEmployeeTrendResponse getEmployeeTrend(PlatformEmployeeTrendRequest request);

    @OperationType("com.sme.platform.analytics.plan.distribution")
    PlatformPlanDistributionResponse getPlanDistribution(PlatformPlanDistributionRequest request);

    @OperationType("com.sme.platform.analytics.forecast")
    PlatformForecastResponse getForecast(PlatformForecastRequest request);

    @OperationType("com.sme.platform.analytics.onboarding.trend")
    PlatformOnboardingTrendResponse getOnboardingTrend(PlatformOnboardingTrendRequest request);

    @OperationType("com.sme.platform.dashboard.risk")
    PlatformRiskDashboardResponse getRiskDashboard(PlatformRiskDashboardRequest request);

    // ============================================================
    // Platform Global Onboarding Template
    // ============================================================

    @OperationType("com.sme.platform.template.create")
    CreatePlatformTemplateResponse createPlatformTemplate(CreatePlatformTemplateRequest request);

    @OperationType("com.sme.platform.template.list")
    ListPlatformTemplateResponse listPlatformTemplate(ListPlatformTemplateRequest request);

    @OperationType("com.sme.platform.template.detail")
    PlatformTemplateDetailResponse getPlatformTemplateDetail(PlatformTemplateDetailRequest request);

    @OperationType("com.sme.platform.template.update")
    CreatePlatformTemplateResponse updatePlatformTemplate(UpdatePlatformTemplateRequest request);

    /** Activate a PLATFORM onboarding template. */
    @OperationType("com.sme.platform.template.activate")
    CreatePlatformTemplateResponse activatePlatformTemplate(ActivatePlatformTemplateRequest request);

    /** Deactivate a PLATFORM onboarding template. Usually ACTIVE/DRAFT -> ARCHIVED. */
    @OperationType("com.sme.platform.template.deactivate")
    CreatePlatformTemplateResponse deactivatePlatformTemplate(DeactivatePlatformTemplateRequest request);

    /** Delete only unused PLATFORM onboarding template. Used templates should be archived instead. */
    @OperationType("com.sme.platform.template.delete")
    DeletePlatformTemplateResponse deletePlatformTemplate(DeletePlatformTemplateRequest request);

    @OperationType("com.sme.platform.errorLog.list")
    PlatformErrorLogListResponse listPlatformErrorLogs(PlatformErrorLogListRequest request);
}
