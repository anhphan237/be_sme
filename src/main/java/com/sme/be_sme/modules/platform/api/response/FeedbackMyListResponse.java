package com.sme.be_sme.modules.platform.api.response;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class FeedbackMyListResponse {

    private List<FeedbackItem> items = new ArrayList<>();
    private Long total = 0L;
    private Integer page;
    private Integer size;

    @Data
    public static class FeedbackItem {

        private String feedbackId;

        private String subject;
        private String content;
        private String status;

        private OffsetDateTime resolvedAt;
        private String resolvedBy;
        private String resolvedByName;
        private String resolutionNote;

        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }
}