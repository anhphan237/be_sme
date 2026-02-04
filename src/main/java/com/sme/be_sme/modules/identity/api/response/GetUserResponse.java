package com.sme.be_sme.modules.identity.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class GetUserResponse {

    // users
    private String userId;
    private String email;
    private String fullName;
    private String phone;
    private String status;

    // employee_profiles
    private String employeeId;
    private String departmentId;
    private String employeeCode;
    private String employeeName;
    private String employeeEmail;
    private String employeePhone;
    private String jobTitle;
    private String managerUserId;
    private Date startDate;
    private String workLocation;
    private String employeeStatus;
}
