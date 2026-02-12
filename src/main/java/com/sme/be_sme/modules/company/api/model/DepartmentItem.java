package com.sme.be_sme.modules.company.api.model;

import lombok.Data;

@Data
public class DepartmentItem {
    private String departmentId;
    private String name;
    private String type;
    private String managerUserId;
}
