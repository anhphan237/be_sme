package com.sme.be_sme.modules.onboarding.api.request;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskScheduleRescheduleRequest {
    private String taskId;
    private Date scheduledStartAt;
    private Date scheduledEndAt;
    private String reason;
}

