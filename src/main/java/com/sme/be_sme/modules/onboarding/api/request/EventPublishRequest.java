package com.sme.be_sme.modules.onboarding.api.request;

import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventPublishRequest {
    private String eventTemplateId;
    private Date eventAt;
    private Date eventEndAt;
    private List<String> departmentIds;
    private List<String> userIds;
}
