package com.sme.be_sme.modules.billing.facade.impl;

import com.sme.be_sme.modules.billing.api.request.InvoiceGenerateRequest;
import com.sme.be_sme.modules.billing.api.request.InvoiceGetRequest;
import com.sme.be_sme.modules.billing.api.request.InvoiceListRequest;
import com.sme.be_sme.modules.billing.api.request.PlanListRequest;
import com.sme.be_sme.modules.billing.api.request.SubscriptionCreateRequest;
import com.sme.be_sme.modules.billing.api.request.SubscriptionGetCurrentRequest;
import com.sme.be_sme.modules.billing.api.request.SubscriptionUpdateRequest;
import com.sme.be_sme.modules.billing.api.request.UsageSummaryRequest;
import com.sme.be_sme.modules.billing.api.request.UsageTrackRequest;
import com.sme.be_sme.modules.billing.api.response.InvoiceDetailResponse;
import com.sme.be_sme.modules.billing.api.response.InvoiceGenerateResponse;
import com.sme.be_sme.modules.billing.api.response.InvoiceListResponse;
import com.sme.be_sme.modules.billing.api.response.PlanListResponse;
import com.sme.be_sme.modules.billing.api.response.SubscriptionCurrentResponse;
import com.sme.be_sme.modules.billing.api.response.SubscriptionResponse;
import com.sme.be_sme.modules.billing.api.response.UsageSummaryResponse;
import com.sme.be_sme.modules.billing.api.response.UsageTrackResponse;
import com.sme.be_sme.modules.billing.facade.BillingFacade;
import com.sme.be_sme.modules.billing.processor.InvoiceGetProcessor;
import com.sme.be_sme.modules.billing.processor.InvoiceGenerateProcessor;
import com.sme.be_sme.modules.billing.processor.InvoiceListProcessor;
import com.sme.be_sme.modules.billing.processor.PlanListProcessor;
import com.sme.be_sme.modules.billing.processor.SubscriptionCreateProcessor;
import com.sme.be_sme.modules.billing.processor.SubscriptionGetCurrentProcessor;
import com.sme.be_sme.modules.billing.processor.SubscriptionUpdateProcessor;
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
    private final UsageSummaryProcessor usageSummaryProcessor;
    private final InvoiceGenerateProcessor invoiceGenerateProcessor;
    private final InvoiceListProcessor invoiceListProcessor;
    private final InvoiceGetProcessor invoiceGetProcessor;
    private final PlanListProcessor planListProcessor;

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
    public PlanListResponse listPlans(PlanListRequest request) {
        return call(planListProcessor, request, PlanListResponse.class);
    }
}
