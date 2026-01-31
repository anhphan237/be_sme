package com.sme.be_sme.modules.billing.facade.impl;

import com.sme.be_sme.modules.billing.api.request.InvoiceGenerateRequest;
import com.sme.be_sme.modules.billing.api.request.SubscriptionCreateRequest;
import com.sme.be_sme.modules.billing.api.request.SubscriptionUpdateRequest;
import com.sme.be_sme.modules.billing.api.request.UsageTrackRequest;
import com.sme.be_sme.modules.billing.api.response.InvoiceGenerateResponse;
import com.sme.be_sme.modules.billing.api.response.SubscriptionResponse;
import com.sme.be_sme.modules.billing.api.response.UsageTrackResponse;
import com.sme.be_sme.modules.billing.facade.BillingFacade;
import com.sme.be_sme.modules.billing.processor.InvoiceGenerateProcessor;
import com.sme.be_sme.modules.billing.processor.SubscriptionCreateProcessor;
import com.sme.be_sme.modules.billing.processor.SubscriptionUpdateProcessor;
import com.sme.be_sme.modules.billing.processor.UsageTrackProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BillingFacadeImpl extends BaseOperationFacade implements BillingFacade {

    private final SubscriptionCreateProcessor subscriptionCreateProcessor;
    private final SubscriptionUpdateProcessor subscriptionUpdateProcessor;
    private final UsageTrackProcessor usageTrackProcessor;
    private final InvoiceGenerateProcessor invoiceGenerateProcessor;

    @Override
    public SubscriptionResponse createSubscription(SubscriptionCreateRequest request) {
        return call(subscriptionCreateProcessor, request, SubscriptionResponse.class);
    }

    @Override
    public SubscriptionResponse updateSubscription(SubscriptionUpdateRequest request) {
        return call(subscriptionUpdateProcessor, request, SubscriptionResponse.class);
    }

    @Override
    public UsageTrackResponse trackUsage(UsageTrackRequest request) {
        return call(usageTrackProcessor, request, UsageTrackResponse.class);
    }

    @Override
    public InvoiceGenerateResponse generateInvoice(InvoiceGenerateRequest request) {
        return call(invoiceGenerateProcessor, request, InvoiceGenerateResponse.class);
    }
}
