package com.sme.be_sme.modules.platform.facade;

import com.sme.be_sme.modules.platform.api.request.FeedbackSubmitRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformActivityLogListRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformCompanyDetailRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformCompanyListRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformCompanyStatusRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformErrorLogListRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformFeedbackDetailRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformFeedbackListRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformFeedbackResolveRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformFinancialDashboardRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformInvoiceListRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformOnboardingAnalyticsRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformPaymentListRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformPlanCreateRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformPlanDeleteRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformPlanListRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformPlanUpdateRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformSubscriptionDetailRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformSubscriptionListRequest;
import com.sme.be_sme.modules.platform.api.request.PlatformSystemHealthRequest;
import com.sme.be_sme.modules.platform.api.response.FeedbackSubmitResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformActivityLogListResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformCompanyDetailResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformCompanyListResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformCompanyStatusResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformErrorLogListResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformFeedbackDetailResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformFeedbackListResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformFeedbackResolveResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformFinancialDashboardResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformInvoiceListResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformOnboardingAnalyticsResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformPaymentListResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformPlanListResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformPlanResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformSubscriptionDetailResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformSubscriptionListResponse;
import com.sme.be_sme.modules.platform.api.response.PlatformSystemHealthResponse;
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
}
