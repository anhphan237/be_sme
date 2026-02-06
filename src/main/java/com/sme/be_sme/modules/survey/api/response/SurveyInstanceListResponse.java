package com.sme.be_sme.modules.survey.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class SurveyInstanceListResponse {
    private List<SurveyInstanceItem> items;
    private int totalCount;

    @Getter
    @Setter
    public static class SurveyInstanceItem {
        private String id;
        private String templateId;
        private String templateName;
        private Date scheduledAt;
        private String status;
        private Date createdAt;
    }
}
