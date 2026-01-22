package com.sme.be_sme.modules.identity.facade;

import com.sme.be_sme.modules.identity.api.request.CreateUserRequest;
import com.sme.be_sme.modules.identity.api.response.CreateUserResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface UserFacade extends OperationFacadeProvider {
    @OperationType("com.sme.identity.user.create")
    CreateUserResponse createUser(CreateUserRequest request);
}
