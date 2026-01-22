package com.sme.be_sme.modules.identity.facade;

import com.sme.be_sme.modules.identity.api.request.CreateUserRequest;
import com.sme.be_sme.modules.identity.api.request.UpdateUserRequest;
import com.sme.be_sme.modules.identity.api.response.CreateUserResponse;
import com.sme.be_sme.modules.identity.api.response.UpdateUserResponse;
import com.sme.be_sme.modules.identity.processor.CreateUserProcessor;
import com.sme.be_sme.modules.identity.processor.IdentityUserCreateProcessor;
import com.sme.be_sme.modules.identity.processor.IdentityUserUpdateProcessor;
import com.sme.be_sme.shared.gateway.core.BizContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFacadeImpl implements UserFacade {

    private final IdentityUserCreateProcessor identityUserCreateProcessor;
    private final IdentityUserUpdateProcessor identityUserUpdateProcessor;

    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {
        return identityUserCreateProcessor.process(BizContextHolder.get(), request);
    }

    @Override
    public UpdateUserResponse updateUser(UpdateUserRequest request) {
        return identityUserUpdateProcessor.process(BizContextHolder.get(), request);
    }
}
