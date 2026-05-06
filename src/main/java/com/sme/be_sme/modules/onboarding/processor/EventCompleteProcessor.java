package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.EventCompleteRequest;
import com.sme.be_sme.modules.onboarding.api.response.EventCompleteResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.EventInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventInstanceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventCompleteProcessor extends BaseBizProcessor<BizContext> {
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final ObjectMapper objectMapper;
    private final EventInstanceMapper eventInstanceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        EventCompleteRequest request =
                objectMapper.convertValue(payload, EventCompleteRequest.class);

        validate(context, request);

        if (!canCompleteEvent(context)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "only HR can complete event");
        }

        String companyId = context.getTenantId().trim();
        String eventInstanceId = request.getEventInstanceId().trim();

        EventInstanceEntity event =
                eventInstanceMapper.selectByCompanyIdAndEventInstanceId(companyId, eventInstanceId);

        if (event == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "event instance not found");
        }

        String currentStatus = normalizeStatus(event.getStatus());

        if (STATUS_CANCELLED.equals(currentStatus)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "cancelled event cannot be completed");
        }

        Date now = new Date();

        if (event.getEventAt() != null && event.getEventAt().after(now)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "event has not started yet");
        }

        if (!STATUS_COMPLETED.equals(currentStatus)) {
            int updated = eventInstanceMapper.updateStatusByCompanyIdAndEventInstanceId(
                    companyId,
                    eventInstanceId,
                    STATUS_COMPLETED,
                    now
            );

            if (updated <= 0) {
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "complete event failed");
            }
        }

        EventCompleteResponse response = new EventCompleteResponse();
        response.setEventInstanceId(eventInstanceId);
        response.setStatus(STATUS_COMPLETED);
        response.setCompletedAt(now);

        return response;
    }

    private static void validate(BizContext context, EventCompleteRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }

        if (!StringUtils.hasText(request.getEventInstanceId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "eventInstanceId is required");
        }
    }

    private static boolean canCompleteEvent(BizContext context) {
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

    private static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }
}