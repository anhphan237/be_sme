package com.sme.be_sme.modules.identity.facade;

import com.sme.be_sme.modules.identity.api.request.AssignRoleRequest;
import com.sme.be_sme.modules.identity.api.request.RevokeRoleRequest;
import com.sme.be_sme.modules.identity.api.response.AssignRoleResponse;
import com.sme.be_sme.modules.identity.api.response.RevokeRoleResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface RoleFacade extends OperationFacadeProvider {

    @OperationType("com.sme.identity.role.assign")
    AssignRoleResponse assignRoleToUser(AssignRoleRequest request);

    @OperationType("com.sme.identity.role.revoke")
    RevokeRoleResponse revokeRoleFromUser(RevokeRoleRequest request);
}
