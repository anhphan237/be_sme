package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.onboarding.api.request.TaskTimelineByOnboardingRequest;
import com.sme.be_sme.modules.onboarding.api.response.TaskTimelineByOnboardingResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskAssigneeListRow;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskSlaService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class TaskTimelineByOnboardingProcessor extends BaseBizProcessor<BizContext> {

    private static final String UNASSIGNED_KEY = "__UNASSIGNED__";

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final TaskInstanceMapperExt taskInstanceMapperExt;
    private final UserMapper userMapper;
    private final EmployeeProfileMapperExt employeeProfileMapperExt;
    private final OnboardingTaskSlaService slaService;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        TaskTimelineByOnboardingRequest request =
                objectMapper.convertValue(payload, TaskTimelineByOnboardingRequest.class);
        validate(context, request);

        String onboardingId = request.getOnboardingId().trim();
        OnboardingInstanceEntity instance = onboardingInstanceMapper.selectByPrimaryKey(onboardingId);
        if (instance == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "onboarding instance not found");
        }
        if (!context.getTenantId().equals(instance.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "instance does not belong to tenant");
        }
        enforceEmployeeOwnInstanceOnly(context, instance);

        Boolean includeDone = Boolean.TRUE.equals(request.getIncludeDone());
        List<TaskAssigneeListRow> rows =
                taskInstanceMapperExt.selectTimelineByOnboardingId(context.getTenantId(), onboardingId, includeDone);

        Map<String, String> userNames = loadAssigneeNames(rows);
        return buildResponse(onboardingId, rows, userNames, slaService);
    }

    private static void validate(BizContext context, TaskTimelineByOnboardingRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getOnboardingId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "onboardingId is required");
        }
    }

    private static TaskTimelineByOnboardingResponse buildResponse(
            String onboardingId,
            List<TaskAssigneeListRow> rows,
            Map<String, String> userNames,
            OnboardingTaskSlaService slaService) {
        List<TaskAssigneeListRow> safeRows = rows == null ? List.of() : rows;
        Map<String, List<TaskAssigneeListRow>> grouped = safeRows.stream()
                .collect(Collectors.groupingBy(
                        row -> StringUtils.hasText(row.getAssignedUserId()) ? row.getAssignedUserId() : UNASSIGNED_KEY,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<TaskTimelineByOnboardingResponse.AssigneeTimeline> assignees = new ArrayList<>();
        for (Map.Entry<String, List<TaskAssigneeListRow>> entry : grouped.entrySet()) {
            String assigneeUserId = UNASSIGNED_KEY.equals(entry.getKey()) ? null : entry.getKey();
            List<TaskAssigneeListRow> assigneeRows = entry.getValue();

            TaskTimelineByOnboardingResponse.AssigneeTimeline timeline =
                    new TaskTimelineByOnboardingResponse.AssigneeTimeline();
            timeline.setAssigneeUserId(assigneeUserId);
            timeline.setAssigneeUserName(assigneeUserId == null ? "Unassigned" : userNames.get(assigneeUserId));

            List<TaskTimelineByOnboardingResponse.TaskItem> items = assigneeRows.stream()
                    .map(row -> toItem(row, slaService))
                    .collect(Collectors.toList());
            timeline.setTasks(items);
            timeline.setTaskCount(items.size());
            assignees.add(timeline);
        }

        TaskTimelineByOnboardingResponse response = new TaskTimelineByOnboardingResponse();
        response.setOnboardingId(onboardingId);
        response.setTotalTasks(safeRows.size());
        response.setAssignees(assignees);
        return response;
    }

    private static TaskTimelineByOnboardingResponse.TaskItem toItem(
            TaskAssigneeListRow row, OnboardingTaskSlaService slaService) {
        TaskTimelineByOnboardingResponse.TaskItem item = new TaskTimelineByOnboardingResponse.TaskItem();
        item.setTaskId(row.getTaskId());
        item.setChecklistId(row.getChecklistId());
        item.setChecklistName(row.getChecklistName());
        item.setTitle(row.getTitle());
        item.setStatus(row.getStatus());
        item.setDueDate(row.getDueDate());
        item.setScheduledStartAt(row.getScheduledStartAt());
        item.setScheduledEndAt(row.getScheduledEndAt());
        item.setScheduleStatus(row.getScheduleStatus());
        item.setScheduleRescheduleReason(row.getScheduleRescheduleReason());
        item.setScheduleCancelReason(row.getScheduleCancelReason());
        item.setScheduleNoShowReason(row.getScheduleNoShowReason());
        item.setCreatedAt(row.getCreatedAt());
        long dueInHours = slaService.dueInHours(row.getDueDate());
        item.setDueInHours(dueInHours == Long.MAX_VALUE ? null : dueInHours);
        item.setOverdue(slaService.isOverdue(row.getDueDate(), row.getStatus()));
        item.setDueCategory(slaService.dueCategory(row.getDueDate(), row.getStatus()));
        item.setRequireAck(row.getRequireAck());
        item.setRequireDoc(row.getRequireDoc());
        item.setRequiresManagerApproval(row.getRequiresManagerApproval());
        item.setApprovalStatus(row.getApprovalStatus());
        return item;
    }

    private Map<String, String> loadAssigneeNames(List<TaskAssigneeListRow> rows) {
        List<String> userIds = (rows == null ? List.<TaskAssigneeListRow>of() : rows).stream()
                .map(TaskAssigneeListRow::getAssignedUserId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<UserEntity> users = userMapper.selectByUserIds(userIds);
        return users.stream()
                .filter(Objects::nonNull)
                .filter(u -> StringUtils.hasText(u.getUserId()))
                .collect(Collectors.toMap(UserEntity::getUserId, UserEntity::getFullName, (a, b) -> a));
    }

    private void enforceEmployeeOwnInstanceOnly(BizContext context, OnboardingInstanceEntity instance) {
        if (!isEmployeeRole(context)) return;
        if (context == null || !StringUtils.hasText(context.getOperatorId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "employee context is required");
        }
        EmployeeProfileEntity me = employeeProfileMapperExt.selectByCompanyIdAndUserId(
                context.getTenantId(),
                context.getOperatorId()
        );
        if (me == null) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "employee profile not found");
        }
        String instanceOnboardeeUserId =
                StringUtils.hasText(instance.getEmployeeId()) ? instance.getEmployeeId().trim() : null;
        String myUserId = context.getOperatorId().trim();
        if (!myUserId.equals(instanceOnboardeeUserId)) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "employee can only access own onboarding instance");
        }
    }

    private boolean isEmployeeRole(BizContext context) {
        if (context == null || context.getRoles() == null) return false;
        return context.getRoles().stream().anyMatch(r -> "EMPLOYEE".equalsIgnoreCase(r));
    }
}

