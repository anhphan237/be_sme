package com.sme.be_sme.modules.platform.api.response;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class PlatformFeedbackListResponse {
    private List<FeedbackItem> items;
    private int total;

    @Data
    public static class FeedbackItem {
        private String feedbackId;
        private String companyId;
        private String companyName;
        private String userId;
        private String subject;
        private String status;
        private Date createdAt;
    }
}
