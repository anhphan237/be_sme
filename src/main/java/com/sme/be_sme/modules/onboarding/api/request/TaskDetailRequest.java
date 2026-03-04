package com.sme.be_sme.modules.onboarding.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskDetailRequest {
    private String taskId;               // Required - ID của task
    private Boolean includeComments;     // Optional - Include comments (default: true)
    private Boolean includeAttachments;  // Optional - Include attachments (default: true)
    private Boolean includeActivityLogs; // Optional - Include activity logs (default: false)
}
