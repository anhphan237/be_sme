package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskDepartmentDependentListRequest {
    private String departmentId;
    /** Optional checkpoint status: PENDING/CONFIRMED. Default: PENDING (non-confirmed). */
    private String checkpointStatus;
    private Integer page;
    private Integer size;
}
