package com.sme.be_sme.modules.chat.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.chat.api.request.ChatSessionListRequest;
import com.sme.be_sme.modules.chat.api.response.ChatSessionListResponse;
import com.sme.be_sme.modules.knowledge.infrastructure.mapper.ChatSessionMapper;
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
public class ChatSessionListProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final ChatSessionMapper chatSessionMapper;

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

        ChatSessionListResponse response = new ChatSessionListResponse();
        response.setSessions(sessions.stream()
                .map(s -> {
                    ChatSessionListResponse.ChatSessionItem item = new ChatSessionListResponse.ChatSessionItem();
                    item.setChatSessionId(s.getChatSessionId());
                    item.setChannel(s.getChannel());
                    item.setStartedAt(s.getStartedAt() != null ? s.getStartedAt().toInstant() : null);
                    item.setEndedAt(s.getEndedAt() != null ? s.getEndedAt().toInstant() : null);
                    return item;
                })
                .collect(Collectors.toList()));
        return response;
    }
}
