package com.sme.be_sme.modules.identity.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String accessToken;
    private String userId;
    private String tenantId;
}
