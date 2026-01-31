package com.sme.be_sme.shared.gateway.core;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class BizContext {
    // ===== request meta =====
    private String requestId;
    private String operationType;

    // ===== auth context =====
    private String tenantId;     // companyId
    private String operatorId;   // userId from JWT
    private Set<String> roles;   // roles from JWT

    // ===== payload =====
    private JsonNode payload;

    public static BizContext of(String requestId, String operationType, JsonNode payload) {
        BizContext ctx = new BizContext();
        ctx.requestId = requestId;
        ctx.operationType = operationType;
        ctx.payload = payload;
        return ctx;
    }

    public static BizContext internal(String tenantId, String requestId, String operatorId, Set<String> roles) {
        BizContext ctx = new BizContext();
        ctx.setTenantId(tenantId);
        ctx.setRequestId(requestId);
        ctx.setOperatorId(operatorId);
        ctx.setRoles(roles);
        return ctx;
    }

}
