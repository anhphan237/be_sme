package com.sme.be_sme.modules.identity.invite.api;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteSetPasswordRequest {

    @NotBlank(message = "token is required")
    private String token;

    @NotBlank(message = "password is required")
    private String password;
}
