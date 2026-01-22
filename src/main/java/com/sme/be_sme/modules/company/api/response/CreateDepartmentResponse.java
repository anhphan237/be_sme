package com.sme.be_sme.modules.company.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDepartmentResponse {
    private String departmentId;
    private String companyId;
    private String name;
    private String status;
}
