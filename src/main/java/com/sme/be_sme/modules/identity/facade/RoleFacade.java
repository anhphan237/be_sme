package com.sme.be_sme.modules.identity.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.modules.identity.api.request.AssignRoleRequest;
import com.sme.be_sme.modules.identity.api.response.AssignRoleResponse;
import com.sme.be_sme.shared.gateway.api.OperationStubResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface RoleFacade extends OperationFacadeProvider {

    @OperationType("com.sme.identity.role.assign")
    AssignRoleResponse assignRoleToUser(AssignRoleRequest request);

    @OperationType("com.sme.identity.role.revoke")
    OperationStubResponse revokeRoleFromUser(JsonNode payload);
}
