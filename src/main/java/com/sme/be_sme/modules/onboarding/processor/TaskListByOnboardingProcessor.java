package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.TaskListByOnboardingRequest;
import com.sme.be_sme.modules.onboarding.api.response.TaskListByOnboardingResponse;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.ChecklistInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.ChecklistInstanceEntity;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskSlaService;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskWorkflow;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskListByOnboardingProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final TaskInstanceMapperExt taskInstanceMapperExt;
    private final ChecklistInstanceMapper checklistInstanceMapper;
    private final EmployeeProfileMapperExt employeeProfileMapperExt;
    private final OnboardingTaskSlaService slaService;

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        TaskListByOnboardingRequest request = objectMapper.convertValue(payload, TaskListByOnboardingRequest.class);
        
        // 1. Validate Input
        validate(context, request);
        
        // 2. Check onboarding exists & belongs to tenant
        OnboardingInstanceEntity instance = onboardingInstanceMapper.selectByPrimaryKey(request.getOnboardingId().trim());
        if (instance == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "onboarding instance not found");
        }
        
        // Get tenant ID from context or from instance (for auth disabled mode)
        String tenantId = context.getTenantId() != null ? context.getTenantId() : instance.getCompanyId();
        
        // Verify tenant ownership if auth is enabled
        if (context.getTenantId() != null && !context.getTenantId().equals(instance.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "instance does not belong to tenant");
        }
        enforceEmployeeOwnInstanceOnly(context, instance);

        // 3. Prepare pagination and sorting
        int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : DEFAULT_PAGE;
        int size = request.getSize() != null && request.getSize() > 0 ? Math.min(request.getSize(), MAX_SIZE) : DEFAULT_SIZE;
        int offset = (page - 1) * size;
        String sortBy = StringUtils.hasText(request.getSortBy()) ? request.getSortBy() : "created_at";
        String sortOrder = "DESC".equalsIgnoreCase(request.getSortOrder()) ? "DESC" : "ASC";

        // 4. Query tasks from DB
        String normalizedStatus = normalizeStatus(request.getStatus());
        List<TaskInstanceEntity> tasks = taskInstanceMapperExt.selectByOnboardingId(
            tenantId,
            request.getOnboardingId().trim(),
            normalizedStatus,
            request.getAssignedUserId(),
            sortBy,
            sortOrder,
            offset,
            size
        );

        Integer totalCount = taskInstanceMapperExt.countByOnboardingId(
            tenantId,
            request.getOnboardingId().trim(),
            normalizedStatus,
            request.getAssignedUserId()
        );

        // 5. Enrich with checklist names
        Map<String, String> checklistNameMap = loadChecklistNames(tenantId, tasks);

        // 6. Build Response
        return buildResponse(request.getOnboardingId().trim(), tasks, checklistNameMap, totalCount, page, size);
    }

    private void validate(BizContext context, TaskListByOnboardingRequest request) {
        if (!StringUtils.hasText(request.getOnboardingId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "onboardingId is required");
        }
        if (request.getOnboardingId().trim().length() > 255) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "onboardingId is too long");
        }
        if (request.getStatus() != null && !isValidStatus(request.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid status value");
        }
        if (request.getSortBy() != null && !isValidSortBy(request.getSortBy())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid sortBy value");
        }
        if (request.getPage() != null && request.getPage() < 1) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "page must be greater than 0");
        }
        if (request.getSize() != null && request.getSize() < 1) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "size must be greater than 0");
        }
    }

    private boolean isValidStatus(String status) {
        return OnboardingTaskWorkflow.isKnownStatus(status) || "PENDING".equalsIgnoreCase(status);
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return status;
        }
        return OnboardingTaskWorkflow.normalizeStatus(status);
    }

    private boolean isValidSortBy(String sortBy) {
        return "due_date".equals(sortBy) || "created_at".equals(sortBy) || "status".equals(sortBy);
    }

    private Map<String, String> loadChecklistNames(String companyId, List<TaskInstanceEntity> tasks) {
        // Get unique checklist IDs
        Set<String> checklistIds = tasks.stream()
            .map(TaskInstanceEntity::getChecklistId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        Map<String, String> result = new HashMap<>();
        if (checklistIds.isEmpty()) {
            return result;
        }
        List<ChecklistInstanceEntity> checklists =
                checklistInstanceMapper.selectByCompanyIdAndChecklistIds(companyId, new ArrayList<>(checklistIds));
        for (ChecklistInstanceEntity checklist : checklists) {
            if (checklist != null && StringUtils.hasText(checklist.getChecklistId())) {
                result.put(checklist.getChecklistId(), checklist.getName());
            }
        }
        return result;
    }

    private TaskListByOnboardingResponse buildResponse(
        String onboardingId,
        List<TaskInstanceEntity> entities,
        Map<String, String> checklistNameMap,
        Integer totalCount,
        int page,
        int size
    ) {
        TaskListByOnboardingResponse response = new TaskListByOnboardingResponse();
        response.setOnboardingId(onboardingId);
        response.setTotalCount(totalCount);
        response.setPage(page);
        response.setSize(size);

        List<TaskListByOnboardingResponse.TaskItem> items = entities.stream()
            .map(e -> {
                TaskListByOnboardingResponse.TaskItem item = new TaskListByOnboardingResponse.TaskItem();
                item.setTaskId(e.getTaskId());
                item.setChecklistId(e.getChecklistId());
                item.setChecklistName(checklistNameMap.get(e.getChecklistId()));
                item.setTitle(e.getTitle());
                item.setDescription(e.getDescription());
                item.setStatus(e.getStatus());
                item.setDueDate(e.getDueDate());
                item.setAssignedUserId(e.getAssignedUserId());
                item.setAssignedDepartmentId(e.getAssignedDepartmentId());
                item.setCompletedAt(e.getCompletedAt());
                item.setCreatedAt(e.getCreatedAt());
                item.setScheduledStartAt(e.getScheduledStartAt());
                item.setScheduledEndAt(e.getScheduledEndAt());
                item.setScheduleStatus(e.getScheduleStatus());
                item.setScheduleRescheduleReason(e.getScheduleRescheduleReason());
                item.setScheduleCancelReason(e.getScheduleCancelReason());
                item.setScheduleNoShowReason(e.getScheduleNoShowReason());
                long dueInHours = slaService.dueInHours(e.getDueDate());
                item.setDueInHours(dueInHours == Long.MAX_VALUE ? null : dueInHours);
                item.setOverdue(slaService.isOverdue(e.getDueDate(), e.getStatus()));
                item.setDueCategory(slaService.dueCategory(e.getDueDate(), e.getStatus()));
                item.setRequireAck(e.getRequireAck());
                item.setRequiresManagerApproval(e.getRequiresManagerApproval());
                item.setApprovalStatus(e.getApprovalStatus());
                item.setApproverUserId(e.getApproverUserId());
                return item;
            })
            .collect(Collectors.toList());

        response.setTasks(items);
        return response;
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
        String instanceOnboardeeUserId = StringUtils.hasText(instance.getEmployeeId()) ? instance.getEmployeeId().trim() : null;
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
