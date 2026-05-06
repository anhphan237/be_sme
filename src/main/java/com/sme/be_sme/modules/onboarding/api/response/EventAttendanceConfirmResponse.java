package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Data;

@Data
public class EventAttendanceConfirmResponse {
    private String eventInstanceId;
    private String userId;
    private Boolean attended;
    private String taskStatus;
    private Integer updatedTaskCount;
}