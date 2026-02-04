package com.sme.be_sme.modules.billing.facade;

import com.sme.be_sme.modules.billing.api.request.InvoiceGenerateRequest;
import com.sme.be_sme.modules.billing.api.request.SubscriptionCreateRequest;
import com.sme.be_sme.modules.billing.api.request.SubscriptionUpdateRequest;
import com.sme.be_sme.modules.billing.api.request.UsageTrackRequest;
import com.sme.be_sme.modules.billing.api.response.InvoiceGenerateResponse;
import com.sme.be_sme.modules.billing.api.response.SubscriptionResponse;
import com.sme.be_sme.modules.billing.api.response.UsageTrackResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface BillingFacade extends OperationFacadeProvider {

    @OperationType("com.sme.billing.subscription.create")
    SubscriptionResponse createSubscription(SubscriptionCreateRequest request);

    @OperationType("com.sme.billing.subscription.update")
    SubscriptionResponse updateSubscription(SubscriptionUpdateRequest request);

    @OperationType("com.sme.billing.usage.track")
    UsageTrackResponse trackUsage(UsageTrackRequest request);

    @OperationType("com.sme.billing.invoice.generate")
    InvoiceGenerateResponse generateInvoice(InvoiceGenerateRequest request);
}
