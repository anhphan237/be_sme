package com.sme.be_sme.modules.employee.api.request;

import lombok.*;

import java.util.Date;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UpsertEmployeeProfileRequest {
    private String userId;
    private String departmentId;

    private String employeeCode;
    private String employeeName;
    private String employeeEmail;
    private String employeePhone;

    private String jobTitle;
    private String managerUserId;
    private Date startDate;
    private String workLocation;

    private String status; // ACTIVE/INACTIVE
}
