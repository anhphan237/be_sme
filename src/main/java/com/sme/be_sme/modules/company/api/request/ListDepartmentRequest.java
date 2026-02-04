package com.sme.be_sme.modules.company.api.request;

import lombok.Data;

@Data
public class ListDepartmentRequest {
    private String status; // ACTIVE | INACTIVE | null
}
