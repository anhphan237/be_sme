package com.sme.be_sme.modules.company.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDepartmentTypeResponse {
    private String departmentTypeId;
    private String companyId;
    private String code;
    private String name;
    private String status;
}
