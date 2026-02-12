package com.sme.be_sme.modules.identity.facade.impl;

import com.sme.be_sme.modules.identity.api.request.CheckEmailRequest;
import com.sme.be_sme.modules.identity.api.request.LoginRequest;
import com.sme.be_sme.modules.identity.api.response.CheckEmailResponse;
import com.sme.be_sme.modules.identity.api.response.LoginResponse;
import com.sme.be_sme.modules.identity.facade.AuthFacade;
import com.sme.be_sme.modules.identity.processor.CheckEmailExistsProcessor;
import com.sme.be_sme.modules.identity.processor.IdentityLoginProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthFacadeImpl extends BaseOperationFacade implements AuthFacade {

    private final IdentityLoginProcessor identityLoginProcessor;
    private final CheckEmailExistsProcessor checkEmailExistsProcessor;

    @Override
    public LoginResponse login(LoginRequest request) {
        return call(identityLoginProcessor, request, LoginResponse.class);
    }

    @Override
    public CheckEmailResponse checkEmailExists(CheckEmailRequest request) {
        return call(checkEmailExistsProcessor, request, CheckEmailResponse.class);
    }
}
