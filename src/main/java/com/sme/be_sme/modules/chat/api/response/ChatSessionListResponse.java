package com.sme.be_sme.modules.chat.api.response;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class ChatSessionListResponse {
    private List<ChatSessionItem> sessions;

    @Getter
    @Setter
    public static class ChatSessionItem {
        private String chatSessionId;
        private String channel;
        private Instant startedAt;
        private Instant endedAt;
    }
}
