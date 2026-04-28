package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.EventTemplateListRequest;
import com.sme.be_sme.modules.onboarding.api.response.EventTemplateListResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.EventTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventTemplateEntity;
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
public class EventTemplateListProcessor extends BaseBizProcessor<BizContext> {
    private final ObjectMapper objectMapper;
    private final EventTemplateMapper eventTemplateMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        objectMapper.convertValue(payload, EventTemplateListRequest.class);
        validate(context);

        List<EventTemplateEntity> entities = eventTemplateMapper.selectByCompanyIdOrderByCreatedAtDesc(
                context.getTenantId().trim()
        );

        EventTemplateListResponse response = new EventTemplateListResponse();
        response.setTotalCount(entities.size());
        response.setItems(entities.stream().map(this::toItem).toList());
        return response;
    }

    private static void validate(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
    }

    private EventTemplateListResponse.Item toItem(EventTemplateEntity entity) {
        EventTemplateListResponse.Item item = new EventTemplateListResponse.Item();
        item.setEventTemplateId(entity.getEventTemplateId());
        item.setName(entity.getName());
        item.setContent(entity.getContent());
        item.setDescription(entity.getDescription());
        item.setStatus(entity.getStatus());
        item.setCreatedBy(entity.getCreatedBy());
        item.setCreatedAt(entity.getCreatedAt());
        item.setUpdatedAt(entity.getUpdatedAt());
        return item;
    }
}
