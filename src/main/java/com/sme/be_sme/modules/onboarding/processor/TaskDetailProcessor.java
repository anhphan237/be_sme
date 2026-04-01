package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.infrastructure.mapper.DepartmentMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentEntity;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.onboarding.api.request.TaskDetailRequest;
import com.sme.be_sme.modules.onboarding.api.response.TaskDetailResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.ChecklistInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskActivityLogMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskAttachmentMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskCommentMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.ChecklistInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskActivityLogEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskAttachmentEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskCommentEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskAuth;
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
public class TaskDetailProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final ChecklistInstanceMapper checklistInstanceMapper;
    private final TaskCommentMapperExt taskCommentMapperExt;
    private final TaskAttachmentMapperExt taskAttachmentMapperExt;
    private final TaskActivityLogMapperExt taskActivityLogMapperExt;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        TaskDetailRequest request = objectMapper.convertValue(payload, TaskDetailRequest.class);

        // 1. Validate Input
        validate(request);

        // 2. Get task by ID
        TaskInstanceEntity task = taskInstanceMapper.selectByPrimaryKey(request.getTaskId().trim());
        if (task == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "task not found");
        }

        // 3. Resolve tenant ID (support auth disabled mode)
        String tenantId = context.getTenantId() != null ? context.getTenantId() : task.getCompanyId();

        // 4. Verify tenant ownership if auth is enabled
        if (context.getTenantId() != null && !context.getTenantId().equals(task.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "task does not belong to tenant");
        }

        if (context.getTenantId() != null
                && StringUtils.hasText(context.getOperatorId())
                && OnboardingTaskAuth.isItStaffScopedToAssignee(context.getRoles())) {
            if (!StringUtils.hasText(task.getAssignedUserId())
                    || !task.getAssignedUserId().equals(context.getOperatorId().trim())) {
                throw AppException.of(ErrorCodes.FORBIDDEN, "only assignee can view this task");
            }
        }

        // 5. Load related data
        ChecklistInstanceEntity checklist = loadChecklist(task.getChecklistId());
        DepartmentEntity department = loadDepartment(task.getAssignedDepartmentId());

        // 6. Load optional collections based on request flags
        List<TaskCommentEntity> comments = shouldIncludeComments(request)
            ? taskCommentMapperExt.selectByTaskId(tenantId, task.getTaskId())
            : Collections.emptyList();

        List<TaskAttachmentEntity> attachments = shouldIncludeAttachments(request)
            ? taskAttachmentMapperExt.selectByTaskId(tenantId, task.getTaskId())
            : Collections.emptyList();

        List<TaskActivityLogEntity> activityLogs = shouldIncludeActivityLogs(request)
            ? taskActivityLogMapperExt.selectByTaskId(tenantId, task.getTaskId())
            : Collections.emptyList();

        // 7. Enrich users for task + comments/attachments/logs in one batch
        Map<String, UserEntity> userMap = loadUsers(task, comments, attachments, activityLogs);
        UserEntity assignedUser = userMap.get(task.getAssignedUserId());
        UserEntity createdByUser = userMap.get(task.getCreatedBy());
        Map<String, String> userNameMap = userMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() != null ? e.getValue().getFullName() : null));

        // 8. Build Response
        return buildResponse(task, checklist, assignedUser, createdByUser, department,
            comments, attachments, activityLogs, userNameMap);
    }

    private void validate(TaskDetailRequest request) {
        if (!StringUtils.hasText(request.getTaskId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "taskId is required");
        }
        if (request.getTaskId().trim().length() > 255) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "taskId is too long");
        }
    }

    private boolean shouldIncludeComments(TaskDetailRequest request) {
        return request.getIncludeComments() == null || request.getIncludeComments();
    }

    private boolean shouldIncludeAttachments(TaskDetailRequest request) {
        return request.getIncludeAttachments() == null || request.getIncludeAttachments();
    }

    private boolean shouldIncludeActivityLogs(TaskDetailRequest request) {
        return request.getIncludeActivityLogs() != null && request.getIncludeActivityLogs();
    }

    private ChecklistInstanceEntity loadChecklist(String checklistId) {
        if (!StringUtils.hasText(checklistId)) {
            return null;
        }
        return checklistInstanceMapper.selectByPrimaryKey(checklistId);
    }

    private DepartmentEntity loadDepartment(String departmentId) {
        if (!StringUtils.hasText(departmentId)) {
            return null;
        }
        return departmentMapper.selectByPrimaryKey(departmentId);
    }

    private Map<String, UserEntity> loadUsers(
        TaskInstanceEntity task,
        List<TaskCommentEntity> comments,
        List<TaskAttachmentEntity> attachments,
        List<TaskActivityLogEntity> activityLogs
    ) {
        // Collect all unique user IDs
        Set<String> userIds = new HashSet<>();
        if (task != null) {
            if (StringUtils.hasText(task.getAssignedUserId())) {
                userIds.add(task.getAssignedUserId());
            }
            if (StringUtils.hasText(task.getCreatedBy())) {
                userIds.add(task.getCreatedBy());
            }
        }

        comments.forEach(c -> {
            if (StringUtils.hasText(c.getCreatedBy())) {
                userIds.add(c.getCreatedBy());
            }
        });

        attachments.forEach(a -> {
            if (StringUtils.hasText(a.getUploadedBy())) {
                userIds.add(a.getUploadedBy());
            }
        });

        activityLogs.forEach(l -> {
            if (StringUtils.hasText(l.getActorUserId())) {
                userIds.add(l.getActorUserId());
            }
        });

        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserEntity> users = userMapper.selectByUserIds(new ArrayList<>(userIds));
        Map<String, UserEntity> result = new HashMap<>();
        for (UserEntity user : users) {
            if (user != null && StringUtils.hasText(user.getUserId())) {
                result.put(user.getUserId(), user);
            }
        }
        return result;
    }

    private TaskDetailResponse buildResponse(
        TaskInstanceEntity task,
        ChecklistInstanceEntity checklist,
        UserEntity assignedUser,
        UserEntity createdByUser,
        DepartmentEntity department,
        List<TaskCommentEntity> comments,
        List<TaskAttachmentEntity> attachments,
        List<TaskActivityLogEntity> activityLogs,
        Map<String, String> userNameMap
    ) {
        TaskDetailResponse response = new TaskDetailResponse();

        // Basic task info
        response.setTaskId(task.getTaskId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus());
        response.setDueDate(task.getDueDate());
        response.setCompletedAt(task.getCompletedAt());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        response.setCreatedBy(task.getCreatedBy());
        response.setRequireAck(task.getRequireAck());
        response.setAcknowledgedAt(task.getAcknowledgedAt());
        response.setAcknowledgedBy(task.getAcknowledgedBy());
        response.setRequiresManagerApproval(task.getRequiresManagerApproval());
        response.setApprovalStatus(task.getApprovalStatus());
        response.setApprovedBy(task.getApprovedBy());
        response.setApprovedAt(task.getApprovedAt());
        response.setRejectionReason(task.getRejectionReason());
        response.setApproverUserId(task.getApproverUserId());

        // Checklist info
        if (checklist != null) {
            TaskDetailResponse.ChecklistInfo checklistInfo = new TaskDetailResponse.ChecklistInfo();
            checklistInfo.setChecklistId(checklist.getChecklistId());
            checklistInfo.setName(checklist.getName());
            checklistInfo.setStage(checklist.getStage());
            checklistInfo.setOnboardingId(checklist.getOnboardingId());
            response.setChecklist(checklistInfo);
        }

        // Assigned user info
        if (assignedUser != null) {
            TaskDetailResponse.UserInfo userInfo = new TaskDetailResponse.UserInfo();
            userInfo.setUserId(assignedUser.getUserId());
            userInfo.setFullName(assignedUser.getFullName());
            userInfo.setEmail(assignedUser.getEmail());
            response.setAssignedUser(userInfo);
        }

        // Created by user info
        if (createdByUser != null) {
            TaskDetailResponse.UserInfo userInfo = new TaskDetailResponse.UserInfo();
            userInfo.setUserId(createdByUser.getUserId());
            userInfo.setFullName(createdByUser.getFullName());
            userInfo.setEmail(createdByUser.getEmail());
            response.setCreatedByUser(userInfo);
        }

        // Department info
        if (department != null) {
            TaskDetailResponse.DepartmentInfo deptInfo = new TaskDetailResponse.DepartmentInfo();
            deptInfo.setDepartmentId(department.getDepartmentId());
            deptInfo.setName(department.getName());
            response.setAssignedDepartment(deptInfo);
        }

        // Comments
        List<TaskDetailResponse.CommentItem> commentItems = comments.stream()
            .map(c -> {
                TaskDetailResponse.CommentItem item = new TaskDetailResponse.CommentItem();
                item.setCommentId(c.getTaskCommentId());
                item.setContent(c.getContent());
                item.setCreatedBy(c.getCreatedBy());
                item.setCreatedByName(userNameMap.get(c.getCreatedBy()));
                item.setCreatedAt(c.getCreatedAt());
                return item;
            })
            .collect(Collectors.toList());
        response.setComments(commentItems);

        // Attachments
        List<TaskDetailResponse.AttachmentItem> attachmentItems = attachments.stream()
            .map(a -> {
                TaskDetailResponse.AttachmentItem item = new TaskDetailResponse.AttachmentItem();
                item.setAttachmentId(a.getTaskAttachmentId());
                item.setFileName(a.getFileName());
                item.setFileUrl(a.getFileUrl());
                item.setFileType(a.getFileType());
                item.setFileSizeBytes(a.getFileSizeBytes());
                item.setUploadedBy(a.getUploadedBy());
                item.setUploadedByName(userNameMap.get(a.getUploadedBy()));
                item.setUploadedAt(a.getUploadedAt());
                return item;
            })
            .collect(Collectors.toList());
        response.setAttachments(attachmentItems);

        // Activity logs
        List<TaskDetailResponse.ActivityLogItem> logItems = activityLogs.stream()
            .map(l -> {
                TaskDetailResponse.ActivityLogItem item = new TaskDetailResponse.ActivityLogItem();
                item.setLogId(l.getTaskActivityLogId());
                item.setAction(l.getAction());
                item.setOldValue(l.getOldValue());
                item.setNewValue(l.getNewValue());
                item.setActorUserId(l.getActorUserId());
                item.setActorName(userNameMap.get(l.getActorUserId()));
                item.setCreatedAt(l.getCreatedAt());
                return item;
            })
            .collect(Collectors.toList());
        response.setActivityLogs(logItems);

        return response;
    }
}
