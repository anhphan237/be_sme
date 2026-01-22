package com.sme.be_sme.modules.identity.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {
    private String email;
    private String password;   // tạm plain, sau này hash ở preCheck/doExecute
    private String fullName;
}

