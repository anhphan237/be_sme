package com.sme.be_sme.modules.identity.api.response;

import lombok.*;

@Getter
@Setter
public class CreateUserResponse {
    private String userId;
    private String email;
    private String fullName;
    private String status;
}
