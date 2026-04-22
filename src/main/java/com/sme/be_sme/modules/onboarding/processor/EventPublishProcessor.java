package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.infrastructure.mapper.DepartmentMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentEntity;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.onboarding.api.request.EventPublishRequest;
import com.sme.be_sme.modules.onboarding.api.response.EventPublishResponse;
import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.ChecklistInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.EventInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.EventTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.ChecklistInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventTemplateEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskWorkflow;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublishProcessor extends BaseBizProcessor<BizContext> {
    private static final String TEMPLATE_TASK_ASSIGNED = "TASK_ASSIGNED";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String SOURCE_TYPE_DEPARTMENT = "DEPARTMENT";
    private static final String SOURCE_TYPE_USER_LIST = "USER_LIST";
    private static final String SOURCE_TYPE_DEPARTMENT_PLUS_USERS = "DEPARTMENT_PLUS_USERS";

    private final ObjectMapper objectMapper;
    private final EventTemplateMapper eventTemplateMapper;
    private final EventInstanceMapper eventInstanceMapper;
    private final ChecklistInstanceMapper checklistInstanceMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final NotificationService notificationService;
    private final DepartmentMapper departmentMapper;
    private final EmployeeProfileMapperExt employeeProfileMapperExt;
    private final UserMapperExt userMapperExt;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        EventPublishRequest request = objectMapper.convertValue(payload, EventPublishRequest.class);
        validate(context, request);

        String companyId = context.getTenantId().trim();
        EventTemplateEntity template =
                eventTemplateMapper.selectByCompanyIdAndTemplateId(companyId, request.getEventTemplateId().trim());
        if (template == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "event template not found");
        }
        if ("INACTIVE".equalsIgnoreCase(template.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "event template is inactive");
        }

        List<String> departmentIds = normalizeIds(request.getDepartmentIds());
        List<String> userIds = normalizeIds(request.getUserIds());
        boolean hasDepartments = !departmentIds.isEmpty();
        boolean hasUsers = !userIds.isEmpty();
        if (!hasDepartments && !hasUsers) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "provide at least one source: departmentIds and/or userIds");
        }

        List<String> participantUserIds;
        String sourceType;
        List<String> departmentUserIds = List.of();
        if (hasDepartments) {
            assertDepartments(companyId, departmentIds);
            departmentUserIds = resolveUsersFromDepartments(companyId, departmentIds);
        }
        if (hasUsers) {
            assertActiveUsers(companyId, userIds);
        }
        if (hasDepartments && hasUsers) {
            Set<String> departmentUserSet = new LinkedHashSet<>(departmentUserIds);
            List<String> duplicateUserIds = userIds.stream()
                    .filter(departmentUserSet::contains)
                    .toList();
            if (!duplicateUserIds.isEmpty()) {
                throw AppException.of(
                        ErrorCodes.BAD_REQUEST,
                        "specified userIds already belong to selected departments, please reassign: "
                                + String.join(", ", duplicateUserIds));
            }
            LinkedHashSet<String> merged = new LinkedHashSet<>(departmentUserIds);
            merged.addAll(userIds);
            participantUserIds = new ArrayList<>(merged);
            sourceType = SOURCE_TYPE_DEPARTMENT_PLUS_USERS;
        } else if (hasDepartments) {
            participantUserIds = departmentUserIds;
            sourceType = SOURCE_TYPE_DEPARTMENT;
        } else {
            participantUserIds = userIds;
            sourceType = SOURCE_TYPE_USER_LIST;
        }
        if (participantUserIds.isEmpty()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "no participants found for this event");
        }

        Date now = new Date();
        String eventInstanceId = UuidGenerator.generate();
        EventInstanceEntity instance = new EventInstanceEntity();
        instance.setEventInstanceId(eventInstanceId);
        instance.setCompanyId(companyId);
        instance.setEventTemplateId(template.getEventTemplateId());
        instance.setEventAt(request.getEventAt());
        instance.setSourceType(sourceType);
        instance.setSourceDepartmentIds(toJsonArray(departmentIds));
        instance.setSourceUserIds(toJsonArray(userIds));
        instance.setParticipantUserIds(toJsonArray(participantUserIds));
        instance.setStatus("PUBLISHED");
        instance.setCreatedBy(context.getOperatorId());
        instance.setCreatedAt(now);
        instance.setUpdatedAt(now);
        if (eventInstanceMapper.insert(instance) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "publish event failed");
        }

        String checklistId = UuidGenerator.generate();
        ChecklistInstanceEntity checklist = new ChecklistInstanceEntity();
        checklist.setChecklistId(checklistId);
        checklist.setCompanyId(companyId);
        checklist.setOnboardingId(eventInstanceId);
        checklist.setName(template.getName());
        checklist.setStage("EVENT");
        checklist.setStatus("NOT_STARTED");
        checklist.setProgressPercent(0);
        checklist.setOpenAt(request.getEventAt());
        checklist.setDeadlineAt(request.getEventAt());
        checklist.setCreatedAt(now);
        checklist.setUpdatedAt(now);
        if (checklistInstanceMapper.insert(checklist) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create event checklist failed");
        }

        String operatorId = StringUtils.hasText(context.getOperatorId()) ? context.getOperatorId().trim() : "system";
        String taskDescription = buildTaskDescription(template);
        for (String participantUserId : participantUserIds) {
            TaskInstanceEntity task = new TaskInstanceEntity();
            task.setTaskId(UuidGenerator.generate());
            task.setCompanyId(companyId);
            task.setChecklistId(checklistId);
            task.setTaskTemplateId(null);
            task.setTitle(template.getName());
            task.setDescription(taskDescription);
            task.setStatus(OnboardingTaskWorkflow.STATUS_ASSIGNED);
            task.setDueDate(request.getEventAt());
            task.setAssignedUserId(participantUserId);
            task.setAssignedDepartmentId(
                    SOURCE_TYPE_DEPARTMENT.equals(sourceType) ? departmentIds.get(0) : null);
            task.setCreatedBy(operatorId);
            task.setCreatedAt(now);
            task.setUpdatedAt(now);
            task.setRequireAck(false);
            task.setRequireDoc(false);
            task.setRequiresManagerApproval(false);
            task.setApprovalStatus(OnboardingTaskWorkflow.APPROVAL_NONE);
            task.setScheduleStatus(OnboardingTaskWorkflow.SCHEDULE_UNSCHEDULED);
            if (taskInstanceMapper.insert(task) != 1) {
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create event task failed");
            }
            notifyTaskAssigned(task, template.getName());
        }

        EventPublishResponse response = new EventPublishResponse();
        response.setEventInstanceId(eventInstanceId);
        response.setEventTemplateId(template.getEventTemplateId());
        response.setEventAt(request.getEventAt());
        response.setTaskCount(participantUserIds.size());
        response.setParticipantUserIds(participantUserIds);
        return response;
    }

    private static void validate(BizContext context, EventPublishRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getEventTemplateId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "eventTemplateId is required");
        }
        if (request.getEventAt() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "eventAt is required");
        }
    }

    private void assertDepartments(String companyId, List<String> departmentIds) {
        for (String departmentId : departmentIds) {
            DepartmentEntity department = departmentMapper.selectByPrimaryKey(departmentId);
            if (department == null || !companyId.equals(department.getCompanyId())) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid departmentId: " + departmentId);
            }
            if (!"ACTIVE".equalsIgnoreCase(department.getStatus())) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "inactive departmentId: " + departmentId);
            }
        }
    }

    private List<String> resolveUsersFromDepartments(String companyId, List<String> departmentIds) {
        if (CollectionUtils.isEmpty(departmentIds)) {
            return List.of();
        }
        List<String> users = employeeProfileMapperExt.selectActiveUserIdsByDepartmentIds(companyId, departmentIds);
        if (CollectionUtils.isEmpty(users)) {
            return List.of();
        }
        return normalizeIds(users);
    }

    private void assertActiveUsers(String companyId, List<String> userIds) {
        for (String userId : userIds) {
            int count = userMapperExt.countActiveByCompanyIdAndUserId(companyId, userId);
            if (count <= 0) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid active userId: " + userId);
            }
        }
    }

    private static List<String> normalizeIds(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String id : ids) {
            if (StringUtils.hasText(id)) {
                normalized.add(id.trim());
            }
        }
        return new ArrayList<>(normalized);
    }

    private String toJsonArray(List<String> ids) {
        try {
            return objectMapper.writeValueAsString(ids == null ? List.of() : ids);
        } catch (JsonProcessingException e) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "serialize event participants failed");
        }
    }

    private static String buildTaskDescription(EventTemplateEntity template) {
        String description = StringUtils.hasText(template.getDescription()) ? template.getDescription().trim() : "";
        String content = StringUtils.hasText(template.getContent()) ? template.getContent().trim() : "";
        if (!StringUtils.hasText(description)) {
            return content;
        }
        if (!StringUtils.hasText(content)) {
            return description;
        }
        return description + "\n\n" + content;
    }

    private void notifyTaskAssigned(TaskInstanceEntity task, String eventName) {
        if (task == null || !StringUtils.hasText(task.getAssignedUserId())) {
            return;
        }
        String taskTitle = StringUtils.hasText(task.getTitle()) ? task.getTitle().trim() : "Event task";
        String dueStr = task.getDueDate() != null
                ? Instant.ofEpochMilli(task.getDueDate().getTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(DATE_FMT)
                : "";
        String eventText = StringUtils.hasText(eventName) ? eventName.trim() : taskTitle;
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("taskTitle", taskTitle);
        placeholders.put("dueDate", dueStr);
        NotificationCreateParams params = NotificationCreateParams.builder()
                .companyId(task.getCompanyId())
                .userId(task.getAssignedUserId())
                .type("TASK_ASSIGNED")
                .title("New event task assigned: " + eventText)
                .content("You have been assigned to event \"" + eventText + "\". Due: " + dueStr)
                .refType("TASK")
                .refId(task.getTaskId())
                .sendEmail(true)
                .emailTemplate(TEMPLATE_TASK_ASSIGNED)
                .emailPlaceholders(placeholders)
                .build();
        try {
            notificationService.create(params);
        } catch (Exception e) {
            log.warn("notify event assignment failed for task {}: {}", task.getTaskId(), e.getMessage());
        }
    }
}
