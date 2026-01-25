package com.sme.be_sme.modules.identity.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.api.request.LoginRequest;
import com.sme.be_sme.modules.identity.api.response.LoginResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdentityLoginProcessor extends BaseBizProcessor<BizContext> {

    private static final long DEFAULT_EXPIRES_SECONDS = 3600L;

    private final ObjectMapper objectMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        objectMapper.convertValue(payload, LoginRequest.class);
        LoginResponse response = new LoginResponse();
        response.setAccessToken("DUMMY_TOKEN");
        response.setTokenType("Bearer");
        response.setExpiresInSeconds(DEFAULT_EXPIRES_SECONDS);
        return response;
    }
}
