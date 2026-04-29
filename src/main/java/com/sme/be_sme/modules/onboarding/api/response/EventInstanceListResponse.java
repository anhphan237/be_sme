package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class EventInstanceListResponse {
    private Integer totalCount;
    private List<Item> items;

    @Getter
    @Setter
    public static class Item {
        private String eventInstanceId;
        private String eventTemplateId;
        private String coverImageUrl;
        private String eventName;
        private String eventDescription;
        private String eventContent;
        private String eventTemplateStatus;
        private Date eventAt;
        private Date eventEndAt;
        private String sourceType;
        private String status;
        private Date notifiedAt;
        private String createdBy;
        private Date createdAt;
        private Date updatedAt;
    }
}
