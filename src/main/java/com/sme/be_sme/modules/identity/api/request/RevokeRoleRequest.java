package com.sme.be_sme.modules.identity.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RevokeRoleRequest {
    private String userId;
    private String roleCode;
}
