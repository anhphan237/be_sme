package com.sme.be_sme.modules.identity.api.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AssignRoleResponse {
    private String userId;
    private String roleCode;
    private String roleId;
    private String status;
}