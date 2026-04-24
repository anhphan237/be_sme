package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.onboarding.api.request.TaskCommentTreeRequest;
import com.sme.be_sme.modules.onboarding.api.response.TaskCommentTreeResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskCommentMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskCommentTreeProcessor extends BaseBizProcessor<BizContext> {

    private static final Comparator<TaskCommentTreeResponse.CommentNode> NODE_ORDER =
            Comparator.comparing(TaskCommentTreeResponse.CommentNode::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(n -> n.getCommentId() != null ? n.getCommentId() : "");

    private final ObjectMapper objectMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final TaskCommentMapperExt taskCommentMapperExt;
    private final UserMapper userMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        TaskCommentTreeRequest request = objectMapper.convertValue(payload, TaskCommentTreeRequest.class);
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || !StringUtils.hasText(request.getTaskId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "taskId is required");
        }

        String companyId = context.getTenantId().trim();
        TaskInstanceEntity task = taskInstanceMapper.selectByPrimaryKey(request.getTaskId().trim());
        if (task == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "task not found");
        }
        if (!companyId.equals(task.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "task does not belong to tenant");
        }

        assertCanViewComments(context, task);

        List<TaskCommentEntity> rows = taskCommentMapperExt.selectByTaskId(companyId, task.getTaskId());
        Map<String, UserEntity> userMap = loadCommentAuthors(rows);
        Map<String, String> userNameMap = userMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() != null ? e.getValue().getFullName() : null));

        Map<String, TaskCommentTreeResponse.CommentNode> byId = new HashMap<>();
        for (TaskCommentEntity c : rows) {
            TaskCommentTreeResponse.CommentNode node = new TaskCommentTreeResponse.CommentNode();
            node.setCommentId(c.getTaskCommentId());
            node.setParentCommentId(c.getParentCommentId());
            node.setContent(c.getContent());
            node.setCreatedBy(c.getCreatedBy());
            node.setCreatedByName(userNameMap.get(c.getCreatedBy()));
            node.setCreatedAt(c.getCreatedAt());
            byId.put(c.getTaskCommentId(), node);
        }

        List<TaskCommentTreeResponse.CommentNode> roots = new ArrayList<>();
        for (TaskCommentEntity c : rows) {
            TaskCommentTreeResponse.CommentNode node = byId.get(c.getTaskCommentId());
            String parentId = c.getParentCommentId();
            if (!StringUtils.hasText(parentId) || !byId.containsKey(parentId.trim())) {
                roots.add(node);
            } else {
                byId.get(parentId.trim()).getChildren().add(node);
            }
        }
        sortRecursive(roots);

        TaskCommentTreeResponse response = new TaskCommentTreeResponse();
        response.setTaskId(task.getTaskId());
        response.setRoots(roots);
        return response;
    }

    private static void assertCanViewComments(BizContext context, TaskInstanceEntity task) {
        if (canViewAllTaskComments(context.getRoles())) {
            return;
        }
        if (StringUtils.hasText(context.getOperatorId())
                && OnboardingTaskAuth.isItStaffScopedToAssignee(context.getRoles())) {
            if (!StringUtils.hasText(task.getAssignedUserId())
                    || !task.getAssignedUserId().equals(context.getOperatorId().trim())) {
                throw AppException.of(ErrorCodes.FORBIDDEN, "only assignee can view comments for this task");
            }
            return;
        }
        if (!StringUtils.hasText(task.getAssignedUserId())
                || !StringUtils.hasText(context.getOperatorId())
                || !task.getAssignedUserId().equals(context.getOperatorId().trim())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "only assignee can view comments for this task");
        }
    }

    private static boolean canViewAllTaskComments(Set<String> roles) {
        if (OnboardingTaskAuth.isHrManagerAdmin(roles)) {
            return true;
        }
        if (roles == null) {
            return false;
        }
        return roles.stream()
                .anyMatch(r -> r != null && "HR_ADMIN".equalsIgnoreCase(r.trim()));
    }

    private Map<String, UserEntity> loadCommentAuthors(List<TaskCommentEntity> comments) {
        Set<String> userIds = new HashSet<>();
        for (TaskCommentEntity c : comments) {
            if (StringUtils.hasText(c.getCreatedBy())) {
                userIds.add(c.getCreatedBy().trim());
            }
        }
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserEntity> users = userMapper.selectByUserIds(new ArrayList<>(userIds));
        Map<String, UserEntity> result = new HashMap<>();
        for (UserEntity u : users) {
            if (u != null && StringUtils.hasText(u.getUserId())) {
                result.put(u.getUserId(), u);
            }
        }
        return result;
    }

    private static void sortRecursive(List<TaskCommentTreeResponse.CommentNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        nodes.sort(NODE_ORDER);
        for (TaskCommentTreeResponse.CommentNode node : nodes) {
            sortRecursive(node.getChildren());
        }
    }
}
