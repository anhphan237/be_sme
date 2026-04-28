package com.sme.be_sme.modules.onboarding.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class EventTemplateListResponse {
    private Integer totalCount;
    private List<Item> items;

    @Getter
    @Setter
    public static class Item {
        private String eventTemplateId;
        private String name;
        private String content;
        private String description;
        private String status;
        private String createdBy;
        private Date createdAt;
        private Date updatedAt;
    }
}
