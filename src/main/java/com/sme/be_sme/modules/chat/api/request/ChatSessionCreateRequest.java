package com.sme.be_sme.modules.chat.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatSessionCreateRequest {
    /** Channel: WEB (default) | MOBILE */
    private String channel;
}
