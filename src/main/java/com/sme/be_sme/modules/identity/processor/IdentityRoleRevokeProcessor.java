package com.sme.be_sme.modules.identity.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.api.request.RevokeRoleRequest;
import com.sme.be_sme.modules.identity.api.response.RevokeRoleResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdentityRoleRevokeProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        RevokeRoleRequest request = objectMapper.convertValue(payload, RevokeRoleRequest.class);
        RevokeRoleResponse response = new RevokeRoleResponse();
        response.setUserId(request.getUserId());
        response.setRoleCode(request.getRoleCode());
        response.setRevoked(true);
        return response;
    }
}
