package com.sme.be_sme.shared.gateway.core;

import com.fasterxml.jackson.databind.JsonNode;

public interface OperationFacade {
    Object execute(String tenantId, String requestId, JsonNode payload);
}
