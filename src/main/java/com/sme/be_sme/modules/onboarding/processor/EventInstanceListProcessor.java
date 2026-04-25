package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.EventInstanceListRequest;
import com.sme.be_sme.modules.onboarding.api.response.EventInstanceListResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.EventInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventInstanceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventInstanceListProcessor extends BaseBizProcessor<BizContext> {
    private final ObjectMapper objectMapper;
    private final EventInstanceMapper eventInstanceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        objectMapper.convertValue(payload, EventInstanceListRequest.class);
        validate(context);

        List<EventInstanceEntity> entities =
                eventInstanceMapper.selectByCompanyIdOrderByEventAtDesc(context.getTenantId().trim());

        EventInstanceListResponse response = new EventInstanceListResponse();
        response.setTotalCount(entities.size());
        response.setItems(entities.stream().map(this::toItem).toList());
        return response;
    }

    private static void validate(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
    }

    private EventInstanceListResponse.Item toItem(EventInstanceEntity entity) {
        EventInstanceListResponse.Item item = new EventInstanceListResponse.Item();
        item.setEventInstanceId(entity.getEventInstanceId());
        item.setEventTemplateId(entity.getEventTemplateId());
        item.setEventAt(entity.getEventAt());
        item.setSourceType(entity.getSourceType());
        item.setStatus(entity.getStatus());
        item.setNotifiedAt(entity.getNotifiedAt());
        item.setCreatedBy(entity.getCreatedBy());
        item.setCreatedAt(entity.getCreatedAt());
        item.setUpdatedAt(entity.getUpdatedAt());
        return item;
    }
}
