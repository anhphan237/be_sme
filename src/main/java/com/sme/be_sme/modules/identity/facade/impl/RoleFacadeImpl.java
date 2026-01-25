package com.sme.be_sme.modules.identity.facade.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.modules.identity.api.request.AssignRoleRequest;
import com.sme.be_sme.modules.identity.api.response.AssignRoleResponse;
import com.sme.be_sme.modules.identity.facade.RoleFacade;
import com.sme.be_sme.modules.identity.processor.IdentityRoleAssignProcessor;
import com.sme.be_sme.shared.gateway.api.OperationStubResponse;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleFacadeImpl extends BaseOperationFacade implements RoleFacade {

    private final IdentityRoleAssignProcessor identityRoleAssignProcessor;

    @Override
    public AssignRoleResponse assignRoleToUser(AssignRoleRequest request) {
        return call(identityRoleAssignProcessor, request, AssignRoleResponse.class);
    }

    @Override
    public OperationStubResponse revokeRoleFromUser(JsonNode payload) {
        return OperationStubResponse.notImplemented("com.sme.identity.role.revoke");
    }
}
