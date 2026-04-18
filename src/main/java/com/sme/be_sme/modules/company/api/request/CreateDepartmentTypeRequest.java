package com.sme.be_sme.modules.company.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDepartmentTypeRequest {
    private String code;
    private String name;
    /** Optional, defaults to ACTIVE */
    private String status;
}
