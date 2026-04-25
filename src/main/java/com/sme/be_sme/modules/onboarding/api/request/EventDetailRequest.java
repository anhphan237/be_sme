package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventDetailRequest {
    private String eventInstanceId;
    private Boolean includeTasks;
}
