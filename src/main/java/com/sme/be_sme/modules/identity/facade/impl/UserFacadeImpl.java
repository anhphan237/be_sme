package com.sme.be_sme.modules.identity.facade.impl;

import com.sme.be_sme.modules.identity.api.request.CreateUserRequest;
import com.sme.be_sme.modules.identity.api.request.DisableUserRequest;
import com.sme.be_sme.modules.identity.api.request.GetUserRequest;
import com.sme.be_sme.modules.identity.api.request.UpdateUserRequest;
import com.sme.be_sme.modules.identity.api.request.UserListRequest;
import com.sme.be_sme.modules.identity.api.response.CreateUserResponse;
import com.sme.be_sme.modules.identity.api.response.DisableUserResponse;
import com.sme.be_sme.modules.identity.api.response.GetUserResponse;
import com.sme.be_sme.modules.identity.api.response.UpdateUserResponse;
import com.sme.be_sme.modules.identity.api.response.UserListResponse;
import com.sme.be_sme.modules.identity.context.IdentityUpdateUserContext;
import com.sme.be_sme.modules.identity.facade.UserFacade;
import com.sme.be_sme.modules.identity.processor.IdentityUserCreateProcessor;
import com.sme.be_sme.modules.identity.processor.IdentityUserDisableProcessor;
import com.sme.be_sme.modules.identity.processor.IdentityUserGetProcessor;
import com.sme.be_sme.modules.identity.processor.IdentityUserListProcessor;
import com.sme.be_sme.modules.identity.processor.IdentityUserUpdateProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFacadeImpl extends BaseOperationFacade implements UserFacade {

    private final IdentityUserCreateProcessor identityUserCreateProcessor;
    private final IdentityUserListProcessor identityUserListProcessor;
    private final IdentityUserGetProcessor identityUserGetProcessor;
    private final IdentityUserUpdateProcessor identityUserUpdateProcessor;
    private final IdentityUserDisableProcessor identityUserDisableProcessor;

    @Override
    public CreateUserResponse createUser(CreateUserRequest request) {
        return call(identityUserCreateProcessor, request, CreateUserResponse.class);
    }

    @Override
    public UserListResponse listUsers(UserListRequest request) {
        return call(identityUserListProcessor, request, UserListResponse.class);
    }

    @Override
    public GetUserResponse getUser(GetUserRequest request) {
        return call(identityUserGetProcessor, request, GetUserResponse.class);
    }

    @Override
    public UpdateUserResponse updateUser(UpdateUserRequest request) {
        return call(
                identityUserUpdateProcessor,
                request,
                UpdateUserResponse.class,
                IdentityUpdateUserContext::new);
    }

    @Override
    public DisableUserResponse disableUser(DisableUserRequest request) {
        return call(identityUserDisableProcessor, request, DisableUserResponse.class);
    }
}
