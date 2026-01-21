package com.sme.be_sme.shared.gateway.core;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class BaseFacade implements OperationFacade {

    @Override
    public final Object execute(String tenantId, String requestId, JsonNode payload) {
        if (skip(tenantId, requestId, payload)) return null;

        preCheck(tenantId, requestId, payload);
        Object result = doExecute(tenantId, requestId, payload);
        postProcess(tenantId, requestId, payload, result);

        return result;
    }

    protected boolean skip(String tenantId, String requestId, JsonNode payload) { return false; }

    protected void preCheck(String tenantId, String requestId, JsonNode payload) {}

    protected void postProcess(String tenantId, String requestId, JsonNode payload, Object result) {}

    protected abstract Object doExecute(String tenantId, String requestId, JsonNode payload);
}
