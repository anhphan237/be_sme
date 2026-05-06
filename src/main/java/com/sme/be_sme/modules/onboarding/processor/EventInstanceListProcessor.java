package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.EventInstanceListRequest;
import com.sme.be_sme.modules.onboarding.api.response.EventInstanceListResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.EventInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.EventTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventInstanceListProcessor extends BaseBizProcessor<BizContext> {
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final EventInstanceMapper eventInstanceMapper;
    private final EventTemplateMapper eventTemplateMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        objectMapper.convertValue(payload, EventInstanceListRequest.class);
        validate(context);

        String companyId = context.getTenantId().trim();

        List<EventInstanceEntity> allEntities =
                eventInstanceMapper.selectByCompanyIdOrderByEventAtDesc(companyId);

        List<EventInstanceEntity> entities = canViewAllEvents(context)
                ? allEntities
                : allEntities.stream()
                .filter(entity -> isParticipant(entity, context.getOperatorId()))
                .toList();

        Map<String, EventTemplateEntity> templateMap = loadTemplateMap(companyId, entities);

        EventInstanceListResponse response = new EventInstanceListResponse();
        response.setTotalCount(entities.size());
        response.setItems(entities.stream().map(e -> toItem(e, templateMap)).toList());

        return response;
    }

    private static void validate(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
    }

    private boolean isParticipant(EventInstanceEntity entity, String operatorId) {
        if (entity == null || !StringUtils.hasText(operatorId)) {
            return false;
        }

        String currentUserId = operatorId.trim();

        return parseJsonArray(entity.getParticipantUserIds()).stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .anyMatch(currentUserId::equals);
    }

    private static boolean canViewAllEvents(BizContext context) {
        Set<String> roles = context.getRoles() == null
                ? Collections.emptySet()
                : context.getRoles();

        Set<String> rolesUpper = roles.stream()
                .filter(StringUtils::hasText)
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        return rolesUpper.contains("ADMIN")
                || rolesUpper.contains("PLATFORM_ADMIN")
                || rolesUpper.contains("COMPANY_ADMIN")
                || rolesUpper.contains("HR")
                || rolesUpper.contains("HR_ADMIN");
    }

    private EventInstanceListResponse.Item toItem(
            EventInstanceEntity entity,
            Map<String, EventTemplateEntity> templateMap
    ) {
        EventInstanceListResponse.Item item = new EventInstanceListResponse.Item();

        EventTemplateEntity template = templateMap.get(entity.getEventTemplateId());

        item.setEventInstanceId(entity.getEventInstanceId());
        item.setEventTemplateId(entity.getEventTemplateId());
        item.setCoverImageUrl(entity.getCoverImageUrl());
        item.setEventName(template == null ? null : template.getName());
        item.setEventDescription(template == null ? null : template.getDescription());
        item.setEventContent(template == null ? null : template.getContent());
        item.setEventTemplateStatus(template == null ? null : template.getStatus());
        item.setEventAt(entity.getEventAt());
        item.setEventEndAt(entity.getEventEndAt());
        item.setSourceType(entity.getSourceType());
        item.setStatus(entity.getStatus());
        item.setNotifiedAt(entity.getNotifiedAt());
        item.setCreatedBy(entity.getCreatedBy());
        item.setCreatedAt(entity.getCreatedAt());
        item.setUpdatedAt(entity.getUpdatedAt());

        return item;
    }

    private Map<String, EventTemplateEntity> loadTemplateMap(
            String companyId,
            List<EventInstanceEntity> entities
    ) {
        Set<String> templateIds = new LinkedHashSet<>();

        for (EventInstanceEntity entity : entities) {
            if (entity != null && StringUtils.hasText(entity.getEventTemplateId())) {
                templateIds.add(entity.getEventTemplateId().trim());
            }
        }

        if (templateIds.isEmpty()) {
            return Map.of();
        }

        List<EventTemplateEntity> templates = eventTemplateMapper.selectByCompanyIdAndTemplateIds(
                companyId,
                new ArrayList<>(templateIds)
        );

        return templates.stream()
                .filter(t -> t != null && StringUtils.hasText(t.getEventTemplateId()))
                .collect(Collectors.toMap(
                        EventTemplateEntity::getEventTemplateId,
                        t -> t,
                        (a, b) -> a
                ));
    }

    private List<String> parseJsonArray(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }

        try {
            return objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }
}