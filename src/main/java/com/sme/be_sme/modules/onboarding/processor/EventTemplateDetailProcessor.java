package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.EventTemplateDetailRequest;
import com.sme.be_sme.modules.onboarding.api.response.EventTemplateDetailResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.EventTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class EventTemplateDetailProcessor extends BaseBizProcessor<BizContext> {
    private final ObjectMapper objectMapper;
    private final EventTemplateMapper eventTemplateMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        EventTemplateDetailRequest request = objectMapper.convertValue(payload, EventTemplateDetailRequest.class);
        validate(context, request);

        EventTemplateEntity entity = eventTemplateMapper.selectByCompanyIdAndTemplateId(
                context.getTenantId().trim(),
                request.getEventTemplateId().trim()
        );
        if (entity == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "event template not found");
        }
        return toResponse(entity);
    }

    private static void validate(BizContext context, EventTemplateDetailRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getEventTemplateId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "eventTemplateId is required");
        }
    }

    private EventTemplateDetailResponse toResponse(EventTemplateEntity entity) {
        EventTemplateDetailResponse response = new EventTemplateDetailResponse();
        response.setEventTemplateId(entity.getEventTemplateId());
        response.setName(entity.getName());
        response.setContent(entity.getContent());
        response.setDescription(entity.getDescription());
        response.setStatus(entity.getStatus());
        response.setCreatedBy(entity.getCreatedBy());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
