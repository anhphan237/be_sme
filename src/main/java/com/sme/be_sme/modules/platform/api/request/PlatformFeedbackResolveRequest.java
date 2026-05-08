package com.sme.be_sme.modules.platform.api.request;

import lombok.Data;

@Data
public class PlatformFeedbackResolveRequest {
    private String feedbackId;
    private String status;
    private String resolutionNote;
}