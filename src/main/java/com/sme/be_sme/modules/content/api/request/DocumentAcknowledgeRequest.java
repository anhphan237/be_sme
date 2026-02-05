package com.sme.be_sme.modules.content.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentAcknowledgeRequest {
    private String documentId;
    /** Onboarding instance id (to link ack to instance and find "Read Handbook" task) */
    private String onboardingId;
    /** Optional: if provided, this task is set to DONE; otherwise backend finds task by document/title */
    private String taskId;
}
