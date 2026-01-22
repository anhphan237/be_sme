package com.sme.be_sme.modules.identity.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.modules.identity.api.request.CreateUserRequest;
import com.sme.be_sme.modules.identity.api.request.DisableUserRequest;
import com.sme.be_sme.modules.identity.api.request.UpdateUserRequest;
import com.sme.be_sme.modules.identity.api.response.CreateUserResponse;
import com.sme.be_sme.modules.identity.api.response.DisableUserResponse;
import com.sme.be_sme.modules.identity.api.response.UpdateUserResponse;
import com.sme.be_sme.modules.identity.processor.CreateUserProcessor;
import com.sme.be_sme.modules.identity.processor.IdentityUserCreateProcessor;
import com.sme.be_sme.modules.identity.processor.IdentityUserDisableProcessor;
import com.sme.be_sme.modules.identity.processor.IdentityUserUpdateProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.gateway.core.BizContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFacadeImpl extends BaseOperationFacade implements UserFacade {

    private final IdentityUserCreateProcessor identityUserCreateProcessor;
    private final IdentityUserUpdateProcessor identityUserUpdateProcessor;
    private final IdentityUserDisableProcessor identityUserDisableProcessor;

    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {
        return call(identityUserCreateProcessor, request, CreateUserResponse.class);
    }

    @Override
    public UpdateUserResponse updateUser(UpdateUserRequest request) {
        return call(identityUserUpdateProcessor, request, UpdateUserResponse.class);
    }

    @Override
    public DisableUserResponse disableUser(DisableUserRequest request) {
        return call(identityUserDisableProcessor, request, DisableUserResponse.class);
    }
}
