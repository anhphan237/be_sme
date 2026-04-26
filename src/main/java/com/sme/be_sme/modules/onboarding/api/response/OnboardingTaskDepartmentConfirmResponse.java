package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingTaskDepartmentConfirmResponse {
    private String taskId;
    private String departmentId;
    private String checkpointStatus;
    private String taskStatus;
    private boolean allDepartmentsConfirmed;
}
