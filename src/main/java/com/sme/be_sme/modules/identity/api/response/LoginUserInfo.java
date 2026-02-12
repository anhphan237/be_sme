package com.sme.be_sme.modules.identity.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginUserInfo {
    private String id;
    private String fullName;
    private String email;
    private String roleCode;
}
