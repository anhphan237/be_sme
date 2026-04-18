package com.sme.be_sme.modules.company.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDepartmentTypeRequest {
    private String departmentTypeId; // required
    private String code; // required
    private String name; // required
    private String status; // optional
}
