package com.sme.be_sme.modules.billing.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.api.OperationStubResponse;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface BillingFacade extends OperationFacadeProvider {

    @OperationType("com.sme.billing.subscription.create")
    OperationStubResponse createSubscription(JsonNode payload);

    @OperationType("com.sme.billing.subscription.update")
    OperationStubResponse updateSubscription(JsonNode payload);

    @OperationType("com.sme.billing.usage.track")
    OperationStubResponse trackUsage(JsonNode payload);

    @OperationType("com.sme.billing.invoice.generate")
    OperationStubResponse generateInvoice(JsonNode payload);
}
