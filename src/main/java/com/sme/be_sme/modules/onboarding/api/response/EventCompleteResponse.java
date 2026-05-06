package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Data;

import java.util.Date;

@Data
public class EventCompleteResponse {
    private String eventInstanceId;
    private String status;
    private Date completedAt;
}