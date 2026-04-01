package com.sme.be_sme.modules.onboarding.api.response;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskScheduleResponse {
    private String taskId;
    private String scheduleStatus;
    private Date scheduledStartAt;
    private Date scheduledEndAt;
    private String scheduleProposedBy;
    private Date scheduleProposedAt;
    private String scheduleConfirmedBy;
    private Date scheduleConfirmedAt;
    private String scheduleRescheduleReason;
    private String scheduleCancelReason;
    private String scheduleNoShowReason;
}

