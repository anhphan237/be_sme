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
        List<TaskInstanceEntity> tasks = taskInstanceMapperExt.selectByOnboardingId(
            tenantId,
            request.getOnboardingId().trim(),
            request.getStatus(),
            request.getAssignedUserId(),
            sortBy,
            sortOrder,
            offset,
            size
        );

        Integer totalCount = taskInstanceMapperExt.countByOnboardingId(
            tenantId,
            request.getOnboardingId().trim(),
            request.getStatus(),
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
        return "TODO".equals(status) || "IN_PROGRESS".equals(status) || 
               "DONE".equals(status) || "PENDING".equals(status);
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
        for (String checklistId : checklistIds) {
            ChecklistInstanceEntity checklist = checklistInstanceMapper.selectByPrimaryKey(checklistId);
            if (checklist != null) {
                result.put(checklistId, checklist.getName());
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
        if (me == null || !StringUtils.hasText(me.getEmployeeId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "employee profile not found");
        }
        String instanceEmployeeId = StringUtils.hasText(instance.getEmployeeId()) ? instance.getEmployeeId().trim() : null;
        String myEmployeeId = me.getEmployeeId().trim();
        String myUserId = context.getOperatorId().trim();
        // Compatibility: old data may store userId in onboarding_instances.employee_id
        if (!myEmployeeId.equals(instanceEmployeeId) && !myUserId.equals(instanceEmployeeId)) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "employee can only access own onboarding instance");
        }
    }

    private boolean isEmployeeRole(BizContext context) {
        if (context == null || context.getRoles() == null) return false;
        return context.getRoles().stream().anyMatch(r -> "EMPLOYEE".equalsIgnoreCase(r));
    }
}
