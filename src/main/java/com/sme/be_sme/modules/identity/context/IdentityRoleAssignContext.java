package com.sme.be_sme.modules.identity.context;

import com.sme.be_sme.modules.identity.api.request.AssignRoleRequest;
import com.sme.be_sme.modules.identity.api.response.AssignRoleResponse;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdentityRoleAssignContext {
    private BizContext biz;
    private AssignRoleRequest request;
    private AssignRoleResponse response;

    // shared state
    private String roleId;     // lookup from roles
    private String userRoleId; // generated
}