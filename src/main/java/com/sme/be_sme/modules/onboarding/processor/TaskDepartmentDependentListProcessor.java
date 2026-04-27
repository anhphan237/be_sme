package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.infrastructure.mapper.DepartmentMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentEntity;
import com.sme.be_sme.modules.onboarding.api.request.TaskDepartmentDependentListRequest;
import com.sme.be_sme.modules.onboarding.api.response.TaskDepartmentDependentListResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskDepartmentCheckpointMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskDepartmentDependentListRow;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class TaskDepartmentDependentListProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ObjectMapper objectMapper;
    private final DepartmentMapper departmentMapper;
    private final TaskDepartmentCheckpointMapper taskDepartmentCheckpointMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        TaskDepartmentDependentListRequest request =
                payload == null || payload.isNull() || payload.isEmpty()
                        ? new TaskDepartmentDependentListRequest()
                        : objectMapper.convertValue(payload, TaskDepartmentDependentListRequest.class);
        validateContextAndRequest(context, request);

        String companyId = context.getTenantId().trim();
        String operatorId = context.getOperatorId().trim();
        String departmentId = request.getDepartmentId().trim();

        DepartmentEntity department = departmentMapper.selectByPrimaryKey(departmentId);
        if (department == null || !companyId.equals(department.getCompanyId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid department");
        }
        if (!StringUtils.hasText(department.getManagerUserId())
                || !operatorId.equals(department.getManagerUserId().trim())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "only manager of this department may view dependent tasks");
        }

        int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : DEFAULT_PAGE;
        int size = request.getSize() != null && request.getSize() > 0 ? Math.min(request.getSize(), MAX_SIZE) : DEFAULT_SIZE;
        int offset = (page - 1) * size;

        String checkpointStatus = normalizeCheckpointStatus(request.getCheckpointStatus());

        List<TaskDepartmentDependentListRow> rows = taskDepartmentCheckpointMapper.selectDependentTasksByDepartment(
                companyId, departmentId, checkpointStatus, offset, size);
        Integer totalCountObj = taskDepartmentCheckpointMapper.countDependentTasksByDepartment(
                companyId, departmentId, checkpointStatus);
        int totalCount = totalCountObj == null ? 0 : totalCountObj;

        TaskDepartmentDependentListResponse response = new TaskDepartmentDependentListResponse();
        response.setDepartmentId(departmentId);
        response.setTotalCount(totalCount);
        response.setPage(page);
        response.setSize(size);
        response.setTasks(rows.stream().map(this::toItem).toList());
        return response;
    }

    private static void validateContextAndRequest(BizContext context, TaskDepartmentDependentListRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (!StringUtils.hasText(context.getOperatorId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "operatorId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getDepartmentId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "departmentId is required");
        }
        if (request.getPage() != null && request.getPage() < 1) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "page must be greater than 0");
        }
        if (request.getSize() != null && request.getSize() < 1) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "size must be greater than 0");
        }
    }

    private static String normalizeCheckpointStatus(String checkpointStatus) {
        if (!StringUtils.hasText(checkpointStatus)) {
            return null;
        }
        String normalized = checkpointStatus.trim().toUpperCase();
        if (!"PENDING".equals(normalized) && !"CONFIRMED".equals(normalized)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "checkpointStatus must be PENDING or CONFIRMED");
        }
        return normalized;
    }

    private TaskDepartmentDependentListResponse.TaskItem toItem(TaskDepartmentDependentListRow row) {
        TaskDepartmentDependentListResponse.TaskItem item = new TaskDepartmentDependentListResponse.TaskItem();
        item.setTaskId(row.getTaskId());
        item.setOnboardingId(row.getOnboardingId());
        item.setChecklistId(row.getChecklistId());
        item.setChecklistName(row.getChecklistName());
        item.setTitle(row.getTitle());
        item.setTaskStatus(row.getTaskStatus());
        item.setDueDate(row.getDueDate());
        item.setAssignedUserId(row.getAssignedUserId());
        item.setAssignedDepartmentId(row.getAssignedDepartmentId());
        item.setCheckpointStatus(row.getCheckpointStatus());
        item.setRequireEvidence(row.getRequireEvidence());
        item.setConfirmedAt(row.getConfirmedAt());
        return item;
    }
}
