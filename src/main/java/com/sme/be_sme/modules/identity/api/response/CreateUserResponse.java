package com.sme.be_sme.modules.identity.api.response;

import lombok.*;

@Getter
@Setter
public class CreateUserResponse {

    // users
    private String userId;
    private String email;
    private String fullName;
    private String status;

    // employee_profiles
    private String employeeId;

    private String roleCode;
}
