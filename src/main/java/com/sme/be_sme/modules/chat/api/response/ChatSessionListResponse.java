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
        /** Title: first USER message content (truncated) or "Phiên chat mới" if empty */
        private String title;
        private String channel;
        private Instant startedAt;
        private Instant endedAt;
    }
}
