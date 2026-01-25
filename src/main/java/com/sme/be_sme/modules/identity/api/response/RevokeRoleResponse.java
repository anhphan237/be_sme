package com.sme.be_sme.modules.identity.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RevokeRoleResponse {
    private String userId;
    private String roleCode;
    private boolean revoked;
}
