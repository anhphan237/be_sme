package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.EventDetailRequest;
import com.sme.be_sme.modules.onboarding.api.response.EventDetailResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.ChecklistInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.EventInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.EventTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.ChecklistInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventTemplateEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EventDetailProcessor extends BaseBizProcessor<BizContext> {
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final EventInstanceMapper eventInstanceMapper;
    private final EventTemplateMapper eventTemplateMapper;
    private final ChecklistInstanceMapper checklistInstanceMapper;
    private final TaskInstanceMapper taskInstanceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        EventDetailRequest request = objectMapper.convertValue(payload, EventDetailRequest.class);
        validate(context, request);

        String tenantId = context.getTenantId().trim();
        String eventInstanceId = request.getEventInstanceId().trim();
        EventInstanceEntity event = eventInstanceMapper.selectByCompanyIdAndEventInstanceId(tenantId, eventInstanceId);
        if (event == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "event instance not found");
        }

        EventTemplateEntity template = eventTemplateMapper.selectByCompanyIdAndTemplateId(
                tenantId,
                event.getEventTemplateId()
        );
        List<ChecklistInstanceEntity> checklists =
                checklistInstanceMapper.selectByCompanyIdAndOnboardingId(tenantId, eventInstanceId);
        ChecklistInstanceEntity checklist = checklists.isEmpty() ? null : checklists.get(0);

        List<TaskInstanceEntity> tasks = shouldIncludeTasks(request)
                ? taskInstanceMapper.selectByCompanyIdAndOnboardingId(tenantId, eventInstanceId)
                : Collections.emptyList();

        EventDetailResponse response = new EventDetailResponse();
        response.setEventInstanceId(event.getEventInstanceId());
        response.setEventTemplateId(event.getEventTemplateId());
        response.setCoverImageUrl(event.getCoverImageUrl());
        response.setEventAt(event.getEventAt());
        response.setEventEndAt(event.getEventEndAt());
        response.setSourceType(event.getSourceType());
        response.setSourceDepartmentIds(parseJsonArray(event.getSourceDepartmentIds()));
        response.setSourceUserIds(parseJsonArray(event.getSourceUserIds()));
        response.setParticipantUserIds(parseJsonArray(event.getParticipantUserIds()));
        response.setStatus(event.getStatus());
        response.setNotifiedAt(event.getNotifiedAt());
        response.setCreatedBy(event.getCreatedBy());
        response.setCreatedAt(event.getCreatedAt());
        response.setUpdatedAt(event.getUpdatedAt());
        response.setEventTemplate(toTemplateInfo(template));
        response.setChecklist(toChecklistInfo(checklist));
        response.setTasks(tasks.stream().map(this::toTaskItem).toList());
        return response;
    }

    private static void validate(BizContext context, EventDetailRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getEventInstanceId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "eventInstanceId is required");
        }
    }

    private static boolean shouldIncludeTasks(EventDetailRequest request) {
        return request.getIncludeTasks() == null || request.getIncludeTasks();
    }

    private EventDetailResponse.EventTemplateInfo toTemplateInfo(EventTemplateEntity template) {
        if (template == null) {
            return null;
        }
        EventDetailResponse.EventTemplateInfo item = new EventDetailResponse.EventTemplateInfo();
        item.setEventTemplateId(template.getEventTemplateId());
        item.setName(template.getName());
        item.setDescription(template.getDescription());
        item.setContent(template.getContent());
        item.setStatus(template.getStatus());
        return item;
    }

    private EventDetailResponse.ChecklistInfo toChecklistInfo(ChecklistInstanceEntity checklist) {
        if (checklist == null) {
            return null;
        }
        EventDetailResponse.ChecklistInfo item = new EventDetailResponse.ChecklistInfo();
        item.setChecklistId(checklist.getChecklistId());
        item.setName(checklist.getName());
        item.setStage(checklist.getStage());
        item.setStatus(checklist.getStatus());
        item.setProgressPercent(checklist.getProgressPercent());
        item.setOpenAt(checklist.getOpenAt());
        item.setDeadlineAt(checklist.getDeadlineAt());
        return item;
    }

    private EventDetailResponse.TaskItem toTaskItem(TaskInstanceEntity task) {
        EventDetailResponse.TaskItem item = new EventDetailResponse.TaskItem();
        item.setTaskId(task.getTaskId());
        item.setChecklistId(task.getChecklistId());
        item.setTitle(task.getTitle());
        item.setDescription(task.getDescription());
        item.setStatus(task.getStatus());
        item.setDueDate(task.getDueDate());
        item.setAssignedUserId(task.getAssignedUserId());
        item.setAssignedDepartmentId(task.getAssignedDepartmentId());
        item.setCompletedAt(task.getCompletedAt());
        item.setCreatedAt(task.getCreatedAt());
        item.setUpdatedAt(task.getUpdatedAt());
        item.setScheduledStartAt(task.getScheduledStartAt());
        item.setScheduledEndAt(task.getScheduledEndAt());
        item.setScheduleStatus(task.getScheduleStatus());
        return item;
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
