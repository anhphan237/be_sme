package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.onboarding.api.request.EventAttendanceSummaryRequest;
import com.sme.be_sme.modules.onboarding.api.response.EventAttendanceSummaryResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.EventInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskWorkflow;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventAttendanceSummaryProcessor extends BaseBizProcessor<BizContext> {
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final EventInstanceMapper eventInstanceMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final UserMapper userMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        EventAttendanceSummaryRequest request = objectMapper.convertValue(payload, EventAttendanceSummaryRequest.class);
        validate(context, request);

        String tenantId = context.getTenantId().trim();
        String eventInstanceId = request.getEventInstanceId().trim();
        EventInstanceEntity event = eventInstanceMapper.selectByCompanyIdAndEventInstanceId(tenantId, eventInstanceId);
        if (event == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "event instance not found");
        }

        List<String> participantUserIds = parseJsonArray(event.getParticipantUserIds());
        List<TaskInstanceEntity> tasks = taskInstanceMapper.selectByCompanyIdAndOnboardingId(tenantId, eventInstanceId);

        Map<String, AttendanceCounter> counterByUser = new LinkedHashMap<>();
        for (String userId : participantUserIds) {
            if (StringUtils.hasText(userId)) {
                counterByUser.put(userId.trim(), new AttendanceCounter());
            }
        }
        for (TaskInstanceEntity task : tasks) {
            if (task == null || !StringUtils.hasText(task.getAssignedUserId())) {
                continue;
            }
            String userId = task.getAssignedUserId().trim();
            AttendanceCounter counter = counterByUser.computeIfAbsent(userId, k -> new AttendanceCounter());
            counter.totalTaskCount++;
            String status = task.getStatus() == null ? "" : task.getStatus().trim().toUpperCase(Locale.ROOT);
            if (OnboardingTaskWorkflow.STATUS_DONE.equals(status)) {
                counter.doneTaskCount++;
            }
        }

        Map<String, String> userNameMap = loadUserNames(new ArrayList<>(counterByUser.keySet()));
        List<EventAttendanceSummaryResponse.AttendeeItem> attendeeItems = new ArrayList<>();
        int attendedCount = 0;
        for (Map.Entry<String, AttendanceCounter> entry : counterByUser.entrySet()) {
            String userId = entry.getKey();
            AttendanceCounter counter = entry.getValue();
            boolean attended = counter.doneTaskCount > 0;
            if (attended) {
                attendedCount++;
            }

            EventAttendanceSummaryResponse.AttendeeItem item = new EventAttendanceSummaryResponse.AttendeeItem();
            item.setUserId(userId);
            item.setFullName(userNameMap.get(userId));
            item.setAttended(attended);
            item.setDoneTaskCount(counter.doneTaskCount);
            item.setTotalTaskCount(counter.totalTaskCount);
            attendeeItems.add(item);
        }

        int totalInvited = participantUserIds.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList().size();
        int notAttendedCount = Math.max(totalInvited - attendedCount, 0);
        double attendanceRate = totalInvited <= 0 ? 0.0d : (double) attendedCount / (double) totalInvited;

        EventAttendanceSummaryResponse response = new EventAttendanceSummaryResponse();
        response.setEventInstanceId(eventInstanceId);
        response.setTotalInvited(totalInvited);
        response.setAttendedCount(attendedCount);
        response.setNotAttendedCount(notAttendedCount);
        response.setAttendanceRate(attendanceRate);
        response.setAttendees(attendeeItems);
        return response;
    }

    private static void validate(BizContext context, EventAttendanceSummaryRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getEventInstanceId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "eventInstanceId is required");
        }
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

    private Map<String, String> loadUserNames(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserEntity> users = userMapper.selectByUserIds(userIds);
        Map<String, String> result = new HashMap<>();
        for (UserEntity user : users) {
            if (user != null && StringUtils.hasText(user.getUserId())) {
                result.put(user.getUserId(), user.getFullName());
            }
        }
        return result;
    }

    private static class AttendanceCounter {
        private int totalTaskCount;
        private int doneTaskCount;
    }
}
