package com.sme.be_sme.modules.platform.infrastructure.dto;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class PlatformFeedbackRow {

    private String feedbackId;

    private String companyId;
    private String companyName;

    private String userId;
    private String userName;
    private String userEmail;

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