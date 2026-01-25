package com.sme.be_sme.modules.billing.facade.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.modules.billing.facade.BillingFacade;
import com.sme.be_sme.shared.gateway.api.OperationStubResponse;
import org.springframework.stereotype.Component;

@Component
public class BillingFacadeImpl implements BillingFacade {

    @Override
    public OperationStubResponse createSubscription(JsonNode payload) {
        return OperationStubResponse.notImplemented("com.sme.billing.subscription.create");
    }

    @Override
    public OperationStubResponse updateSubscription(JsonNode payload) {
        return OperationStubResponse.notImplemented("com.sme.billing.subscription.update");
    }

    @Override
    public OperationStubResponse trackUsage(JsonNode payload) {
        return OperationStubResponse.notImplemented("com.sme.billing.usage.track");
    }

    @Override
    public OperationStubResponse generateInvoice(JsonNode payload) {
        return OperationStubResponse.notImplemented("com.sme.billing.invoice.generate");
    }
}
