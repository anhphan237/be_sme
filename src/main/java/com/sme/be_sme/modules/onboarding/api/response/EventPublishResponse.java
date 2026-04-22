package com.sme.be_sme.modules.onboarding.api.response;

import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventPublishResponse {
    private String eventInstanceId;
    private String eventTemplateId;
    private Date eventAt;
    private Date eventEndAt;
    private Integer taskCount;
    private List<String> participantUserIds;
}
