package com.sme.be_sme.modules.platform.api.request;

import lombok.Data;

@Data
public class FeedbackSubmitRequest {
    private String subject;
    private String content;
}
