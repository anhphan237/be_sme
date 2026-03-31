package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.TaskListByAssigneeRequest;
import com.sme.be_sme.modules.onboarding.api.response.TaskListByAssigneeResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskAssigneeListRow;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class TaskListByAssigneeProcessor extends BaseBizProcessor<BizContext> {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ObjectMapper objectMapper;
    private final TaskInstanceMapperExt taskInstanceMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        TaskListByAssigneeRequest request =
                payload == null || payload.isNull() || payload.isEmpty()
                        ? new TaskListByAssigneeRequest()
                        : objectMapper.convertValue(payload, TaskListByAssigneeRequest.class);

        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (!StringUtils.hasText(context.getOperatorId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "operatorId is required");
        }

        String companyId = context.getTenantId().trim();
        String assigneeUserId = context.getOperatorId().trim();

        if (request.getPage() != null && request.getPage() < 1) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "page must be greater than 0");
        }
        if (request.getSize() != null && request.getSize() < 1) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "size must be greater than 0");
        }

        int page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : DEFAULT_PAGE;
        int size = request.getSize() != null && request.getSize() > 0 ? Math.min(request.getSize(), MAX_SIZE) : DEFAULT_SIZE;
        int offset = (page - 1) * size;

        String sortBy = StringUtils.hasText(request.getSortBy()) ? request.getSortBy() : "created_at";
        String sortOrder = "DESC".equalsIgnoreCase(request.getSortOrder()) ? "DESC" : "ASC";
        if (!isValidSortBy(sortBy)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid sortBy value");
        }

        String status = StringUtils.hasText(request.getStatus()) ? request.getStatus().trim() : null;
        if (status != null && !isValidStatus(status)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid status value");
        }

        List<TaskAssigneeListRow> rows = taskInstanceMapperExt.selectByCompanyIdAndAssignee(
                companyId, assigneeUserId, status, sortBy, sortOrder, offset, size);
        Integer totalCountObj = taskInstanceMapperExt.countByCompanyIdAndAssignee(companyId, assigneeUserId, status);
        int totalCount = totalCountObj == null ? 0 : totalCountObj;

        TaskListByAssigneeResponse response = new TaskListByAssigneeResponse();
        response.setAssigneeUserId(assigneeUserId);
        response.setTotalCount(totalCount);
        response.setPage(page);
        response.setSize(size);
        response.setTasks(
                rows.stream()
                        .map(this::toItem)
                        .collect(Collectors.toList()));
        return response;
    }

    private TaskListByAssigneeResponse.TaskItem toItem(TaskAssigneeListRow r) {
        TaskListByAssigneeResponse.TaskItem item = new TaskListByAssigneeResponse.TaskItem();
        item.setTaskId(r.getTaskId());
        item.setOnboardingId(r.getOnboardingId());
        item.setChecklistId(r.getChecklistId());
        item.setChecklistName(r.getChecklistName());
        item.setTitle(r.getTitle());
        item.setDescription(r.getDescription());
        item.setStatus(r.getStatus());
        item.setDueDate(r.getDueDate());
        item.setAssignedUserId(r.getAssignedUserId());
        item.setAssignedDepartmentId(r.getAssignedDepartmentId());
        item.setCompletedAt(r.getCompletedAt());
        item.setCreatedAt(r.getCreatedAt());
        item.setRequireAck(r.getRequireAck());
        item.setRequiresManagerApproval(r.getRequiresManagerApproval());
        item.setApprovalStatus(r.getApprovalStatus());
        item.setApproverUserId(r.getApproverUserId());
        return item;
    }

    private static boolean isValidStatus(String status) {
        return "TODO".equals(status)
                || "IN_PROGRESS".equals(status)
                || "DONE".equals(status)
                || "PENDING".equals(status)
                || "PENDING_APPROVAL".equals(status);
    }

    private static boolean isValidSortBy(String sortBy) {
        return "due_date".equals(sortBy) || "created_at".equals(sortBy) || "status".equals(sortBy);
    }
}
