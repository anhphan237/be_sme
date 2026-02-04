package com.sme.be_sme.modules.identity.facade;

import com.sme.be_sme.modules.identity.api.request.LoginRequest;
import com.sme.be_sme.modules.identity.api.response.LoginResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface AuthFacade extends OperationFacadeProvider {

    @OperationType("com.sme.identity.auth.login")
    LoginResponse login(LoginRequest request);
}
