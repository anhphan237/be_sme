package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.EventTemplateCreateRequest;
import com.sme.be_sme.modules.onboarding.api.response.EventTemplateCreateResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.EventTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import java.util.Date;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class EventTemplateCreateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final EventTemplateMapper eventTemplateMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        EventTemplateCreateRequest request = objectMapper.convertValue(payload, EventTemplateCreateRequest.class);
        validate(context, request);

        Date now = new Date();
        EventTemplateEntity entity = new EventTemplateEntity();
        entity.setEventTemplateId(UuidGenerator.generate());
        entity.setCompanyId(context.getTenantId().trim());
        entity.setName(request.getName().trim());
        entity.setContent(request.getContent().trim());
        entity.setDescription(request.getDescription());
        entity.setStatus(normalizeStatus(request.getStatus()));
        entity.setCreatedBy(context.getOperatorId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        if (eventTemplateMapper.insert(entity) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create event template failed");
        }

        EventTemplateCreateResponse response = new EventTemplateCreateResponse();
        response.setEventTemplateId(entity.getEventTemplateId());
        response.setName(entity.getName());
        response.setStatus(entity.getStatus());
        return response;
    }

    private static void validate(BizContext context, EventTemplateCreateRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is required");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "content is required");
        }
    }

    private static String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "ACTIVE";
        }
        String normalized = status.trim().toUpperCase(Locale.US);
        if (!"ACTIVE".equals(normalized) && !"INACTIVE".equals(normalized) && !"DRAFT".equals(normalized)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid status");
        }
        return normalized;
    }
}
