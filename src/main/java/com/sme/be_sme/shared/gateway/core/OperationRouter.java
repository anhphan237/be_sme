package com.sme.be_sme.shared.gateway.core;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OperationRouter {

    private final FacadeRegistry registry;

    public Object route(String operationType, String tenantId, String requestId, JsonNode payload) {
        return registry.get(operationType).execute(tenantId, requestId, payload);
    }
}
