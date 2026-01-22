package com.sme.be_sme.modules.identity.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {
    private String companyId;
    private String email;
    private String password;   // tạm plain, sau này hash ở preCheck/doExecute
    private String fullName;
    private String phone;
    private String status;
}
