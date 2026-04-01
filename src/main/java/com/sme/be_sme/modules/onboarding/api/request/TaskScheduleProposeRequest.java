package com.sme.be_sme.modules.onboarding.api.request;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskScheduleProposeRequest {
    private String taskId;
    private Date scheduledStartAt;
    private Date scheduledEndAt;
}

