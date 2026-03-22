package com.sme.be_sme.modules.chat.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.chat.api.request.ChatMessageListRequest;
import com.sme.be_sme.modules.chat.api.response.ChatMessageListResponse;
import com.sme.be_sme.modules.knowledge.infrastructure.mapper.ChatMessageMapper;
import com.sme.be_sme.modules.knowledge.infrastructure.mapper.ChatSessionMapper;
import com.sme.be_sme.modules.knowledge.infrastructure.persistence.entity.ChatMessageEntity;
import com.sme.be_sme.modules.knowledge.infrastructure.persistence.entity.ChatSessionEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatMessageListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        ChatMessageListRequest request = objectMapper.convertValue(payload, ChatMessageListRequest.class);

        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getChatSessionId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "chatSessionId is required");
        }

        String companyId = context.getTenantId();
        String userId = context.getOperatorId();
        String chatSessionId = request.getChatSessionId().trim();

        ChatSessionEntity session = chatSessionMapper.selectByPrimaryKey(chatSessionId);
        if (session == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "Chat session not found");
        }
        if (!companyId.equals(session.getCompanyId()) || !userId.equals(session.getUserId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "Chat session does not belong to user");
        }

        List<ChatMessageEntity> messages = chatMessageMapper.selectBySessionIdOrderByCreatedAt(chatSessionId);
        if (messages == null) {
            messages = new ArrayList<>();
        }

        ChatMessageListResponse response = new ChatMessageListResponse();
        response.setMessages(messages.stream()
                .map(m -> {
                    ChatMessageListResponse.ChatMessageItem item = new ChatMessageListResponse.ChatMessageItem();
                    item.setChatMessageId(m.getChatMessageId());
                    item.setSender(m.getSender());
                    item.setContent(m.getContent());
                    item.setCreatedAt(m.getCreatedAt() != null ? m.getCreatedAt().toInstant() : null);
                    return item;
                })
                .collect(Collectors.toList()));
        return response;
    }
}
