package com.sme.be_sme.modules.chat.facade.impl;

import com.sme.be_sme.modules.chat.api.request.ChatMessageListRequest;
import com.sme.be_sme.modules.chat.api.request.ChatSessionCreateRequest;
import com.sme.be_sme.modules.chat.api.request.ChatSessionListRequest;
import com.sme.be_sme.modules.chat.api.response.ChatMessageListResponse;
import com.sme.be_sme.modules.chat.api.response.ChatSessionCreateResponse;
import com.sme.be_sme.modules.chat.api.response.ChatSessionListResponse;
import com.sme.be_sme.modules.chat.facade.ChatFacade;
import com.sme.be_sme.modules.chat.processor.ChatMessageListProcessor;
import com.sme.be_sme.modules.chat.processor.ChatSessionCreateProcessor;
import com.sme.be_sme.modules.chat.processor.ChatSessionListProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatFacadeImpl extends BaseOperationFacade implements ChatFacade {

    private final ChatSessionCreateProcessor chatSessionCreateProcessor;
    private final ChatSessionListProcessor chatSessionListProcessor;
    private final ChatMessageListProcessor chatMessageListProcessor;

    @Override
    public ChatSessionCreateResponse createSession(ChatSessionCreateRequest request) {
        return call(chatSessionCreateProcessor, request != null ? request : new ChatSessionCreateRequest(),
                ChatSessionCreateResponse.class);
    }

    @Override
    public ChatSessionListResponse listSessions(ChatSessionListRequest request) {
        return call(chatSessionListProcessor, request != null ? request : new ChatSessionListRequest(),
                ChatSessionListResponse.class);
    }

    @Override
    public ChatMessageListResponse listMessages(ChatMessageListRequest request) {
        return call(chatMessageListProcessor, request, ChatMessageListResponse.class);
    }
}
