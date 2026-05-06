package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.EventAttendanceConfirmRequest;
import com.sme.be_sme.modules.onboarding.api.response.EventAttendanceConfirmResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.EventInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventInstanceEntity;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskWorkflow;
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
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventAttendanceConfirmProcessor extends BaseBizProcessor<BizContext> {
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final ObjectMapper objectMapper;
    private final EventInstanceMapper eventInstanceMapper;
    private final TaskInstanceMapper taskInstanceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        EventAttendanceConfirmRequest request =
                objectMapper.convertValue(payload, EventAttendanceConfirmRequest.class);

        validateBasic(context, request);

        String companyId = context.getTenantId().trim();
        String eventInstanceId = request.getEventInstanceId().trim();

        boolean highPrivilege = canManageEventConfirmation(context);
        String operatorId = StringUtils.hasText(context.getOperatorId())
                ? context.getOperatorId().trim()
                : null;

        String targetUserId = StringUtils.hasText(request.getUserId())
                ? request.getUserId().trim()
                : operatorId;

        if (!StringUtils.hasText(targetUserId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "userId is required");
        }

        // Nếu FE không truyền attended thì mặc định là xác nhận tham gia.
        boolean attended = request.getAttended() == null || Boolean.TRUE.equals(request.getAttended());

        // Employee/Manager chỉ được xác nhận cho chính mình.
        if (!highPrivilege && !targetUserId.equals(operatorId)) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "normal users can only confirm their own event participation"
            );
        }

        EventInstanceEntity event =
                eventInstanceMapper.selectByCompanyIdAndEventInstanceId(companyId, eventInstanceId);

        if (event == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "event instance not found");
        }

        String eventStatus = normalizeStatus(event.getStatus());

        if (STATUS_CANCELLED.equals(eventStatus)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "cancelled event cannot be updated");
        }

        if (STATUS_COMPLETED.equals(eventStatus)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "completed event cannot be updated");
        }

        List<String> participantUserIds = parseJsonArray(event.getParticipantUserIds());

        boolean isParticipant = participantUserIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .anyMatch(targetUserId::equals);

        if (!isParticipant) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "user is not a participant of this event");
        }

        String nextTaskStatus = attended
                ? OnboardingTaskWorkflow.STATUS_DONE
                : OnboardingTaskWorkflow.STATUS_ASSIGNED;

        Date now = new Date();
        Date completedAt = attended ? now : null;

        int updatedCount = taskInstanceMapper.updateEventAttendanceByAssignedUser(
                companyId,
                eventInstanceId,
                targetUserId,
                nextTaskStatus,
                completedAt,
                now
        );

        if (updatedCount <= 0) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "event participant task not found");
        }

        EventAttendanceConfirmResponse response = new EventAttendanceConfirmResponse();
        response.setEventInstanceId(eventInstanceId);
        response.setUserId(targetUserId);
        response.setAttended(attended);
        response.setTaskStatus(nextTaskStatus);
        response.setUpdatedTaskCount(updatedCount);

        return response;
    }

    private static void validateBasic(BizContext context, EventAttendanceConfirmRequest request) {
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

    private static boolean canManageEventConfirmation(BizContext context) {
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