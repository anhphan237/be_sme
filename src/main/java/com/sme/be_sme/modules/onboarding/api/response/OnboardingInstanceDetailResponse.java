package com.sme.be_sme.modules.onboarding.api.response;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingInstanceDetailResponse {
    private String instanceId;
    private String employeeId;
    private String templateId;
    private String status;
    private Date startDate;
    private Date completedAt;
}
