package com.sme.be_sme.modules.platform.api.response;

import java.util.Date;

import lombok.Data;

@Data
public class PlatformFeedbackDetailResponse {
    private String feedbackId;
    private String companyId;
    private String companyName;
    private String userId;
    private String userName;
    private String subject;
    private String content;
    private String status;
    private Date resolvedAt;
    private String resolvedBy;
    private Date createdAt;
}
