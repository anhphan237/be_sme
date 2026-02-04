package com.sme.be_sme.modules.analytics.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentOnboardingStatsResponse {
    private String departmentId;
    private String departmentName;
    private int totalTasks;
    private int completedTasks;
}
