package com.sme.be_sme.modules.chat.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.chat.api.request.ChatSessionCreateRequest;
import com.sme.be_sme.modules.chat.api.response.ChatSessionCreateResponse;
import com.sme.be_sme.modules.knowledge.infrastructure.mapper.ChatSessionMapper;
import com.sme.be_sme.modules.knowledge.infrastructure.persistence.entity.ChatSessionEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class ChatSessionCreateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final ChatSessionMapper chatSessionMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        ChatSessionCreateRequest request = payload != null && !payload.isNull()
                ? objectMapper.convertValue(payload, ChatSessionCreateRequest.class)
                : new ChatSessionCreateRequest();

        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        String companyId = context.getTenantId();
        String userId = context.getOperatorId();
        String channel = StringUtils.hasText(request.getChannel()) ? request.getChannel().trim().toUpperCase() : "WEB";
        if (!"WEB".equals(channel) && !"MOBILE".equals(channel)) {
            channel = "WEB";
        }

        ChatSessionEntity session = new ChatSessionEntity();
        session.setChatSessionId(UuidGenerator.generate());
        session.setCompanyId(companyId);
        session.setUserId(userId);
        session.setChannel(channel);
        session.setStartedAt(new Date());

        if (chatSessionMapper.insert(session) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "failed to create chat session");
        }

        ChatSessionCreateResponse response = new ChatSessionCreateResponse();
        response.setChatSessionId(session.getChatSessionId());
        return response;
    }
}
