package com.sme.be_sme.shared.gateway.core;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

@Getter
public class BizContext {
    private final String tenantId;
    private final String requestId;
    private final JsonNode payload;

    private BizContext(String tenantId, String requestId, JsonNode payload) {
        this.tenantId = tenantId;
        this.requestId = requestId;
        this.payload = payload;
    }

    public static BizContext of(String tenantId, String requestId, JsonNode payload) {
        return new BizContext(tenantId, requestId, payload);
    }

    public static BizContext of(String tenantId, String requestId) {
        return new BizContext(tenantId, requestId, null);
    }
}
