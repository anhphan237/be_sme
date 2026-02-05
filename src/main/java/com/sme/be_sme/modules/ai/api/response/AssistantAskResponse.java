package com.sme.be_sme.modules.ai.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AssistantAskResponse {
    /** Generated answer for the new employee */
    private String answer;
    /** Document names used as context (company-specific) */
    private List<String> sourceDocumentNames;
}
