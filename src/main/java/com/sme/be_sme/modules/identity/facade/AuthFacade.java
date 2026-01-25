package com.sme.be_sme.modules.identity.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.api.OperationStubResponse;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface AuthFacade extends OperationFacadeProvider {

    @OperationType("com.sme.identity.auth.login")
    OperationStubResponse login(JsonNode payload);
}
