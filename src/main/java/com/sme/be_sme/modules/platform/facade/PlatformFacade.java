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
}
