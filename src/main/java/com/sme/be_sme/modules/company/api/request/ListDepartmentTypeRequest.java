package com.sme.be_sme.modules.company.api.request;

import lombok.Data;

@Data
public class ListDepartmentTypeRequest {
    private String status; // ACTIVE | INACTIVE | null
}
