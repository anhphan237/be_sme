package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingTaskDepartmentConfirmRequest {
    private String taskId;
    private String departmentId;
    private String evidenceNote;
    private String evidenceRef;
}
