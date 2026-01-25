package com.sme.be_sme.modules.identity.facade.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.modules.identity.facade.AuthFacade;
import com.sme.be_sme.shared.gateway.api.OperationStubResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthFacadeImpl implements AuthFacade {

    @Override
    public OperationStubResponse login(JsonNode payload) {
        return OperationStubResponse.notImplemented("com.sme.identity.auth.login");
    }
}
