package com.sme.be_sme.modules.platform.facade.impl;

import com.sme.be_sme.modules.platform.api.request.*;
import com.sme.be_sme.modules.platform.api.response.*;
import com.sme.be_sme.modules.platform.facade.PlatformFacade;
import com.sme.be_sme.modules.platform.processor.analytics.PlatformOnboardingAnalyticsProcessor;
import com.sme.be_sme.modules.platform.processor.company.PlatformCompanyActivateProcessor;
import com.sme.be_sme.modules.platform.processor.company.PlatformCompanyDeactivateProcessor;
import com.sme.be_sme.modules.platform.processor.company.PlatformCompanyDeleteProcessor;
import com.sme.be_sme.modules.platform.processor.company.PlatformCompanyDetailProcessor;
import com.sme.be_sme.modules.platform.processor.company.PlatformCompanyListProcessor;
import com.sme.be_sme.modules.platform.processor.dashboard.PlatformFinancialDashboardProcessor;
import com.sme.be_sme.modules.platform.processor.feedback.FeedbackSubmitProcessor;
import com.sme.be_sme.modules.platform.processor.feedback.PlatformFeedbackDetailProcessor;
import com.sme.be_sme.modules.platform.processor.feedback.PlatformFeedbackListProcessor;
import com.sme.be_sme.modules.platform.processor.feedback.PlatformFeedbackResolveProcessor;
import com.sme.be_sme.modules.platform.processor.monitoring.PlatformActivityLogListProcessor;
import com.sme.be_sme.modules.platform.processor.monitoring.PlatformErrorLogListProcessor;
import com.sme.be_sme.modules.platform.processor.monitoring.PlatformSystemHealthProcessor;
import com.sme.be_sme.modules.platform.processor.plan.PlatformPlanCreateProcessor;
import com.sme.be_sme.modules.platform.processor.plan.PlatformPlanDeleteProcessor;
import com.sme.be_sme.modules.platform.processor.plan.PlatformPlanListProcessor;
import com.sme.be_sme.modules.platform.processor.plan.PlatformPlanUpdateProcessor;
import com.sme.be_sme.modules.platform.processor.subscription.PlatformInvoiceListProcessor;
import com.sme.be_sme.modules.platform.processor.subscription.PlatformPaymentListProcessor;
import com.sme.be_sme.modules.platform.processor.subscription.PlatformSubscriptionDetailProcessor;
import com.sme.be_sme.modules.platform.processor.subscription.PlatformSubscriptionListProcessor;
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
}
