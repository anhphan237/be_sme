package com.sme.be_sme.modules.company.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDepartmentRequest {
    private String departmentId; // required
    private String companyId;    // optional, tenant from JWT
    private String name;        // required for update
    private String type;        // optional
    private String status;      // optional
    /** Assign head of department (user_id). Send null or blank to clear. */
    private String managerUserId;
}