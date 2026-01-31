package com.sme.be_sme.modules.company.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDepartmentRequest {
    private String departmentId; // required
    private String companyId;    // required (validate vs BizContext)
    private String name;         // optional
    private String type;         // optional
    private String status;       // optional
}