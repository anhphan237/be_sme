package com.sme.be_sme.modules.identity.api.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class AssignRoleResponse {
    private boolean created;
    private String roleId;
}