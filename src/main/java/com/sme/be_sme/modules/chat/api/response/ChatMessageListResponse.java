package com.sme.be_sme.modules.chat.api.response;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class ChatMessageListResponse {
    private List<ChatMessageItem> messages;

    @Getter
    @Setter
    public static class ChatMessageItem {
        private String chatMessageId;
        private String sender;
        private String content;
        private Instant createdAt;
    }
}
