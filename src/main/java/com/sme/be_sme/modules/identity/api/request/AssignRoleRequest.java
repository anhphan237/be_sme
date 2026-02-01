package com.sme.be_sme.modules.identity.api.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignRoleRequest {
    private String userId;
    private String roleCode; // ADMIN/HR/MANAGER/IT/EMPLOYEE
}
