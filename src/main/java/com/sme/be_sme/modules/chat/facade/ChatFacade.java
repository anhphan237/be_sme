package com.sme.be_sme.modules.chat.facade;

import com.sme.be_sme.modules.chat.api.request.ChatMessageListRequest;
import com.sme.be_sme.modules.chat.api.request.ChatSessionCreateRequest;
import com.sme.be_sme.modules.chat.api.request.ChatSessionListRequest;
import com.sme.be_sme.modules.chat.api.response.ChatMessageListResponse;
import com.sme.be_sme.modules.chat.api.response.ChatSessionCreateResponse;
import com.sme.be_sme.modules.chat.api.response.ChatSessionListResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;

public interface ChatFacade {

    @OperationType("com.sme.chat.session.create")
    ChatSessionCreateResponse createSession(ChatSessionCreateRequest request);

    @OperationType("com.sme.chat.session.list")
    ChatSessionListResponse listSessions(ChatSessionListRequest request);

    @OperationType("com.sme.chat.message.list")
    ChatMessageListResponse listMessages(ChatMessageListRequest request);
}
