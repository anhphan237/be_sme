package com.sme.be_sme.modules.identity.api.request;

import lombok.*;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    /* required */
    private String userId;

    /* users */
    private String email;
    private String fullName;
    private String phone;
    private String status;    // ACTIVE/INACTIVE

    /* password (optional) */
    private String newPassword;

    /* employee_profiles */
    private String departmentId;
    private String employeeCode;
    private String employeeName;   // nếu bạn muốn tách, còn không thì map fullName -> employeeName
    private String employeeEmail;
    private String employeePhone;

    private String jobTitle;
    private String managerUserId;
    private Date startDate;
    private String workLocation;

    private String companyId;
}

