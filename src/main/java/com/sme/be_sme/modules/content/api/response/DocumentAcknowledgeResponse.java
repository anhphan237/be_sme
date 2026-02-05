package com.sme.be_sme.modules.content.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentAcknowledgeResponse {
    private String documentAcknowledgementId;
    private String documentId;
    private String onboardingId;
    /** Whether the linked onboarding task was set to DONE */
    private Boolean taskMarkedDone;
}
