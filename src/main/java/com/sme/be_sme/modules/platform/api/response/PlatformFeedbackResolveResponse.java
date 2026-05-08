package com.sme.be_sme.modules.platform.api.response;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class PlatformFeedbackResolveResponse {
    private String feedbackId;
    private String status;
    private String resolvedBy;
    private OffsetDateTime resolvedAt;
    private String resolutionNote;
}