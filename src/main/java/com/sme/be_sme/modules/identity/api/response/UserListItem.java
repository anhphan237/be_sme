package com.sme.be_sme.modules.identity.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserListItem {
    private String userId;
    private String email;
    private String fullName;
    private String phone;
    private String status;
    /** Role codes (e.g. ADMIN, HR, EMPLOYEE) */
    private List<String> roles;
}
