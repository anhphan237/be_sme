package com.sme.be_sme.modules.ai.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssistantAskRequest {
    /** The new employee's question (e.g. WiFi, parking, regulations) */
    private String question;
    /** Optional: defaults to current operator from context if not provided */
    private String userId;
}
