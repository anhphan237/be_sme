package com.sme.be_sme.modules.chat.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageListRequest {
    /** Required: chat session id */
    private String chatSessionId;
}
