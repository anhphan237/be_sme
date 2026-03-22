package com.sme.be_sme.modules.chat.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.chat.api.request.ChatSessionListRequest;
import com.sme.be_sme.modules.chat.api.response.ChatSessionListResponse;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatSessionListProcessor extends BaseBizProcessor<BizContext> {

    private static final int TITLE_MAX_LENGTH = 80;

    private final ObjectMapper objectMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        ChatSessionListRequest request = payload != null && !payload.isNull()
                ? objectMapper.convertValue(payload, ChatSessionListRequest.class)
                : new ChatSessionListRequest();

        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        String companyId = context.getTenantId();
        String userId = context.getOperatorId();

        List<ChatSessionEntity> sessions = chatSessionMapper.selectByCompanyIdAndUserId(companyId, userId);
        if (sessions == null) {
            sessions = new ArrayList<>();
        }

        List<String> sessionIds = sessions.stream().map(ChatSessionEntity::getChatSessionId).collect(Collectors.toList());
        Map<String, String> sessionIdToTitle = new LinkedHashMap<>();
        if (!sessionIds.isEmpty()) {
            List<ChatMessageEntity> firstMessages = chatMessageMapper.selectFirstMessageBySessionIds(sessionIds);
            if (firstMessages != null) {
                for (ChatMessageEntity m : firstMessages) {
                    if (m.getContent() != null && !m.getContent().isBlank()) {
                        String title = m.getContent().trim();
                        if (title.length() > TITLE_MAX_LENGTH) {
                            title = title.substring(0, TITLE_MAX_LENGTH) + "...";
                        }
                        sessionIdToTitle.put(m.getChatSessionId(), title);
                    }
                }
            }
        }

        Map<String, String> finalTitles = sessionIdToTitle;
        ChatSessionListResponse response = new ChatSessionListResponse();
        response.setSessions(sessions.stream()
                .map(s -> {
                    ChatSessionListResponse.ChatSessionItem item = new ChatSessionListResponse.ChatSessionItem();
                    item.setChatSessionId(s.getChatSessionId());
                    item.setTitle(finalTitles.getOrDefault(s.getChatSessionId(), "Phiên chat mới"));
                    item.setChannel(s.getChannel());
                    item.setStartedAt(s.getStartedAt() != null ? s.getStartedAt().toInstant() : null);
                    item.setEndedAt(s.getEndedAt() != null ? s.getEndedAt().toInstant() : null);
                    return item;
                })
                .collect(Collectors.toList()));
        return response;
    }
}
