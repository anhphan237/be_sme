package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Data;

@Data
public class EventAttendanceConfirmRequest {
    private String eventInstanceId;
    private String userId;
    private Boolean attended;
}