package com.sme.be_sme.modules.onboarding.api.request;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskScheduleCalendarRequest {
    private String userId;
    private Date fromTime;
    private Date toTime;
    private Integer page;
    private Integer size;
}
