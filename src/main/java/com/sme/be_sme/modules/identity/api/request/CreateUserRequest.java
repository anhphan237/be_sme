package com.sme.be_sme.modules.identity.api.request;

import lombok.*;

import java.util.Date;

/**
 * Used by operationType: com.sme.identity.user.create
 *
 * Scope:
 *  - Create user account (users)
 *  - Create employee profile (employee_profiles)
 *  - Assign exactly ONE role (user_roles)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    /* =========================
     * User (users table)
     * ========================= */
    private String email;
    private String password;

    private String fullName;
    private String phone;

    /**
     * Optional, default = ACTIVE
     * Values: ACTIVE / INACTIVE
     */
    private String status;

    /**
     * Optional fallback.
     * In normal flow, tenantId comes from BizContext (JWT).
     */
    private String companyId;

    /* =========================
     * Employee Profile (employee_profiles)
     * ========================= */

    /**
     * Required.
     * Used to set employee_profiles.department_id
     */
    private String departmentId;

    /**
     * Optional HR code (e.g. EMP-2026-001)
     */
    private String employeeCode;

    private String jobTitle;

    /**
     * Manager of this employee (user_id)
     */
    private String managerUserId;

    private Date startDate;

    private String workLocation;

    /* =========================
     * RBAC (user_roles)
     * ========================= */

    /**
     * Required.
     * One of: ADMIN, HR, MANAGER, IT, EMPLOYEE
     *
     * NOTE:
     * - In company.setup: hardcoded ADMIN
     * - In user.create: provided by FE
     */
    private String roleCode;
}

