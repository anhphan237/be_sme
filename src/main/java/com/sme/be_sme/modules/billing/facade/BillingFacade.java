package com.sme.be_sme.modules.billing.facade;

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
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface BillingFacade extends OperationFacadeProvider {

    @OperationType("com.sme.billing.subscription.create")
    SubscriptionResponse createSubscription(SubscriptionCreateRequest request);

    @OperationType("com.sme.billing.subscription.update")
    SubscriptionResponse updateSubscription(SubscriptionUpdateRequest request);

    @OperationType("com.sme.billing.subscription.getCurrent")
    SubscriptionCurrentResponse getCurrentSubscription(SubscriptionGetCurrentRequest request);

    @OperationType("com.sme.billing.usage.track")
    UsageTrackResponse trackUsage(UsageTrackRequest request);

    @OperationType("com.sme.billing.usage.check")
    UsageCheckResponse checkUsage(UsageCheckRequest request);

    @OperationType("com.sme.billing.usage.summary")
    UsageSummaryResponse getUsageSummary(UsageSummaryRequest request);

    @OperationType("com.sme.billing.invoice.generate")
    InvoiceGenerateResponse generateInvoice(InvoiceGenerateRequest request);

    @OperationType("com.sme.billing.invoice.list")
    InvoiceListResponse listInvoices(InvoiceListRequest request);

    @OperationType("com.sme.billing.invoice.get")
    InvoiceDetailResponse getInvoice(InvoiceGetRequest request);

    @OperationType("com.sme.billing.plan.get")
    PlanGetResponse getPlan(PlanGetRequest request);

    @OperationType("com.sme.billing.plan.list")
    PlanListResponse listPlans(PlanListRequest request);

    @OperationType("com.sme.billing.payment.createIntent")
    PaymentCreateIntentResponse createPaymentIntent(PaymentCreateIntentRequest request);

    @OperationType("com.sme.billing.payment.connect")
    PaymentConnectResponse connectPaymentProvider(PaymentConnectRequest request);

    @OperationType("com.sme.billing.payment.providers")
    PaymentProvidersResponse listPaymentProviders(PaymentProvidersRequest request);

    @OperationType("com.sme.billing.payment.status")
    PaymentStatusResponse getPaymentStatus(PaymentStatusRequest request);

    @OperationType("com.sme.billing.payment.transactions")
    PaymentTransactionsResponse listPaymentTransactions(PaymentTransactionsRequest request);

    @OperationType("com.sme.billing.dunning.retry")
    DunningRetryResponse dunningRetry(DunningRetryRequest request);
}
