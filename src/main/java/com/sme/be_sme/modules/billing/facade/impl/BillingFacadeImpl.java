package com.sme.be_sme.modules.billing.facade.impl;

import com.sme.be_sme.modules.billing.api.request.InvoiceGenerateRequest;
import com.sme.be_sme.modules.billing.api.request.InvoiceGetRequest;
import com.sme.be_sme.modules.billing.api.request.InvoiceListRequest;
import com.sme.be_sme.modules.billing.api.request.DunningRetryRequest;
import com.sme.be_sme.modules.billing.api.request.PaymentConnectRequest;
import com.sme.be_sme.modules.billing.api.request.PaymentCreateIntentRequest;
import com.sme.be_sme.modules.billing.api.request.PaymentProvidersRequest;
import com.sme.be_sme.modules.billing.api.request.PaymentStatusRequest;
import com.sme.be_sme.modules.billing.api.request.PaymentTransactionsRequest;
import com.sme.be_sme.modules.billing.api.request.PlanGetRequest;
import com.sme.be_sme.modules.billing.api.request.PlanListRequest;
import com.sme.be_sme.modules.billing.api.request.SubscriptionCreateRequest;
import com.sme.be_sme.modules.billing.api.request.SubscriptionGetCurrentRequest;
import com.sme.be_sme.modules.billing.api.request.SubscriptionUpdateRequest;
import com.sme.be_sme.modules.billing.api.request.UsageCheckRequest;
import com.sme.be_sme.modules.billing.api.request.UsageSummaryRequest;
import com.sme.be_sme.modules.billing.api.request.UsageTrackRequest;
import com.sme.be_sme.modules.billing.api.response.InvoiceDetailResponse;
import com.sme.be_sme.modules.billing.api.response.InvoiceGenerateResponse;
import com.sme.be_sme.modules.billing.api.response.InvoiceListResponse;
import com.sme.be_sme.modules.billing.api.response.DunningRetryResponse;
import com.sme.be_sme.modules.billing.api.response.PaymentConnectResponse;
import com.sme.be_sme.modules.billing.api.response.PaymentCreateIntentResponse;
import com.sme.be_sme.modules.billing.api.response.PaymentProvidersResponse;
import com.sme.be_sme.modules.billing.api.response.PaymentStatusResponse;
import com.sme.be_sme.modules.billing.api.response.PaymentTransactionsResponse;
import com.sme.be_sme.modules.billing.api.response.PlanGetResponse;
import com.sme.be_sme.modules.billing.api.response.PlanListResponse;
import com.sme.be_sme.modules.billing.api.response.SubscriptionCurrentResponse;
import com.sme.be_sme.modules.billing.api.response.SubscriptionResponse;
import com.sme.be_sme.modules.billing.api.response.UsageCheckResponse;
import com.sme.be_sme.modules.billing.api.response.UsageSummaryResponse;
import com.sme.be_sme.modules.billing.api.response.UsageTrackResponse;
import com.sme.be_sme.modules.billing.facade.BillingFacade;
import com.sme.be_sme.modules.billing.processor.InvoiceGetProcessor;
import com.sme.be_sme.modules.billing.processor.InvoiceGenerateProcessor;
import com.sme.be_sme.modules.billing.processor.InvoiceListProcessor;
import com.sme.be_sme.modules.billing.processor.PlanGetProcessor;
import com.sme.be_sme.modules.billing.processor.DunningRetryProcessor;
import com.sme.be_sme.modules.billing.processor.PaymentConnectProcessor;
import com.sme.be_sme.modules.billing.processor.PaymentCreateIntentProcessor;
import com.sme.be_sme.modules.billing.processor.PaymentProvidersProcessor;
import com.sme.be_sme.modules.billing.processor.PaymentStatusProcessor;
import com.sme.be_sme.modules.billing.processor.PaymentTransactionsProcessor;
import com.sme.be_sme.modules.billing.processor.PlanListProcessor;
import com.sme.be_sme.modules.billing.processor.SubscriptionCreateProcessor;
import com.sme.be_sme.modules.billing.processor.SubscriptionGetCurrentProcessor;
import com.sme.be_sme.modules.billing.processor.SubscriptionUpdateProcessor;
import com.sme.be_sme.modules.billing.processor.UsageCheckProcessor;
import com.sme.be_sme.modules.billing.processor.UsageSummaryProcessor;
import com.sme.be_sme.modules.billing.processor.UsageTrackProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BillingFacadeImpl extends BaseOperationFacade implements BillingFacade {

    private final SubscriptionCreateProcessor subscriptionCreateProcessor;
    private final SubscriptionUpdateProcessor subscriptionUpdateProcessor;
    private final SubscriptionGetCurrentProcessor subscriptionGetCurrentProcessor;
    private final UsageTrackProcessor usageTrackProcessor;
    private final UsageCheckProcessor usageCheckProcessor;
    private final UsageSummaryProcessor usageSummaryProcessor;
    private final InvoiceGenerateProcessor invoiceGenerateProcessor;
    private final InvoiceListProcessor invoiceListProcessor;
    private final InvoiceGetProcessor invoiceGetProcessor;
    private final PlanGetProcessor planGetProcessor;
    private final PlanListProcessor planListProcessor;
    private final PaymentCreateIntentProcessor paymentCreateIntentProcessor;
    private final PaymentConnectProcessor paymentConnectProcessor;
    private final PaymentProvidersProcessor paymentProvidersProcessor;
    private final PaymentStatusProcessor paymentStatusProcessor;
    private final PaymentTransactionsProcessor paymentTransactionsProcessor;
    private final DunningRetryProcessor dunningRetryProcessor;

    @Override
    public SubscriptionResponse createSubscription(SubscriptionCreateRequest request) {
        return call(subscriptionCreateProcessor, request, SubscriptionResponse.class);
    }

    @Override
    public SubscriptionResponse updateSubscription(SubscriptionUpdateRequest request) {
        return call(subscriptionUpdateProcessor, request, SubscriptionResponse.class);
    }

    @Override
    public SubscriptionCurrentResponse getCurrentSubscription(SubscriptionGetCurrentRequest request) {
        return call(subscriptionGetCurrentProcessor, request, SubscriptionCurrentResponse.class);
    }

    @Override
    public UsageTrackResponse trackUsage(UsageTrackRequest request) {
        return call(usageTrackProcessor, request, UsageTrackResponse.class);
    }

    @Override
    public UsageCheckResponse checkUsage(UsageCheckRequest request) {
        return call(usageCheckProcessor, request, UsageCheckResponse.class);
    }

    @Override
    public UsageSummaryResponse getUsageSummary(UsageSummaryRequest request) {
        return call(usageSummaryProcessor, request, UsageSummaryResponse.class);
    }

    @Override
    public InvoiceGenerateResponse generateInvoice(InvoiceGenerateRequest request) {
        return call(invoiceGenerateProcessor, request, InvoiceGenerateResponse.class);
    }

    @Override
    public InvoiceListResponse listInvoices(InvoiceListRequest request) {
        return call(invoiceListProcessor, request, InvoiceListResponse.class);
    }

    @Override
    public InvoiceDetailResponse getInvoice(InvoiceGetRequest request) {
        return call(invoiceGetProcessor, request, InvoiceDetailResponse.class);
    }

    @Override
    public PlanGetResponse getPlan(PlanGetRequest request) {
        return call(planGetProcessor, request, PlanGetResponse.class);
    }

    @Override
    public PlanListResponse listPlans(PlanListRequest request) {
        return call(planListProcessor, request, PlanListResponse.class);
    }

    @Override
    public PaymentCreateIntentResponse createPaymentIntent(PaymentCreateIntentRequest request) {
        return call(paymentCreateIntentProcessor, request, PaymentCreateIntentResponse.class);
    }

    @Override
    public PaymentConnectResponse connectPaymentProvider(PaymentConnectRequest request) {
        return call(paymentConnectProcessor, request, PaymentConnectResponse.class);
    }

    @Override
    public PaymentProvidersResponse listPaymentProviders(PaymentProvidersRequest request) {
        return call(paymentProvidersProcessor, request, PaymentProvidersResponse.class);
    }

    @Override
    public PaymentStatusResponse getPaymentStatus(PaymentStatusRequest request) {
        return call(paymentStatusProcessor, request, PaymentStatusResponse.class);
    }

    @Override
    public PaymentTransactionsResponse listPaymentTransactions(PaymentTransactionsRequest request) {
        return call(paymentTransactionsProcessor, request, PaymentTransactionsResponse.class);
    }

    @Override
    public DunningRetryResponse dunningRetry(DunningRetryRequest request) {
        return call(dunningRetryProcessor, request, DunningRetryResponse.class);
    }
}
