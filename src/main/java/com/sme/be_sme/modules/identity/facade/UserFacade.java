package com.sme.be_sme.modules.identity.facade;

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
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface UserFacade extends OperationFacadeProvider {

    @OperationType("com.sme.identity.user.create")
    CreateUserResponse createUser(CreateUserRequest request);

    @OperationType("com.sme.identity.user.list")
    UserListResponse listUsers(UserListRequest request);

    @OperationType("com.sme.identity.user.get")
    GetUserResponse getUser(GetUserRequest request);

    @OperationType("com.sme.identity.user.update")
    UpdateUserResponse updateUser(UpdateUserRequest request);

    @OperationType("com.sme.identity.user.disable")
    DisableUserResponse disableUser(DisableUserRequest request);
}
