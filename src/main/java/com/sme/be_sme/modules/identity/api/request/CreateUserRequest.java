package com.sme.be_sme.modules.identity.api.request;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String email;
    private String password;   // tạm plain, sau này hash ở preCheck/doExecute
    private String fullName;
}

