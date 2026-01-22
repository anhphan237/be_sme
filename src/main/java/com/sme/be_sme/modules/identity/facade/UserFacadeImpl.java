package com.sme.be_sme.modules.identity.facade;

import com.sme.be_sme.modules.identity.api.request.CreateUserRequest;
import com.sme.be_sme.modules.identity.api.response.CreateUserResponse;
import com.sme.be_sme.modules.identity.processor.CreateUserProcessor;
import com.sme.be_sme.shared.gateway.core.BizContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFacadeImpl implements UserFacade {

    private final CreateUserProcessor processor;

    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {
        return processor.process(BizContextHolder.get(), request);
    }
}
