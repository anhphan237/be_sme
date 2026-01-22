package com.sme.be_sme.modules.company.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDepartmentRequest {
    private String departmentId;
    private String companyId;
    private String name;
    private String type;
    private String status;
}
