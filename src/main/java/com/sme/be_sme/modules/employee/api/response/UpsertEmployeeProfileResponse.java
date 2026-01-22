package com.sme.be_sme.modules.employee.api.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class UpsertEmployeeProfileResponse {
    private String employeeId;
    private boolean created; // true nếu insert, false nếu update
}
