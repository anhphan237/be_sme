package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.company.infrastructure.mapper.DepartmentMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentEntity;
import com.sme.be_sme.modules.document.infrastructure.mapper.DocumentMapper;
import com.sme.be_sme.modules.document.infrastructure.persistence.entity.DocumentEntity;
import com.sme.be_sme.modules.onboarding.api.request.ChecklistTemplateUpdateItem;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateUpdateRequest;
import com.sme.be_sme.modules.onboarding.api.request.TaskTemplateUpdateItem;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.ChecklistTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskTemplateDepartmentCheckpointMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskTemplateRequiredDocumentMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.ChecklistTemplateEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskTemplateEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskTemplateDepartmentCheckpointEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskTemplateRequiredDocumentEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingTemplateUpdateProcessor extends BaseBizProcessor<BizContext> {
    private static final String LEVEL_PLATFORM = "PLATFORM";

    private final ObjectMapper objectMapper;
    private final OnboardingTemplateMapper onboardingTemplateMapper;
    private final OnboardingTemplateMapperExt onboardingTemplateMapperExt;
    private final ChecklistTemplateMapper checklistTemplateMapper;
    private final TaskTemplateMapper taskTemplateMapper;
    private final TaskTemplateDepartmentCheckpointMapper taskTemplateDepartmentCheckpointMapper;
    private final TaskTemplateRequiredDocumentMapper taskTemplateRequiredDocumentMapper;
    private final DocumentMapper documentMapper;
    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingTemplateUpdateRequest request = objectMapper.convertValue(payload, OnboardingTemplateUpdateRequest.class);
        validate(context, request);

        String companyId = context.getTenantId();
        OnboardingTemplateEntity entity = onboardingTemplateMapperExt.selectTemplateByIdAndCompany(
                request.getTemplateId(), companyId);
        if (entity == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "onboarding template not found");
        }
        if (LEVEL_PLATFORM.equalsIgnoreCase(entity.getLevel())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "platform template is read-only, please clone before editing");
        }

        if (request.getName() != null) {
            entity.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus().trim());
        }
        entity.setUpdatedAt(new Date());

        int updated = onboardingTemplateMapper.updateByPrimaryKey(entity);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "update onboarding template failed");
        }

        if (request.getStatus() != null) {
            Date now = entity.getUpdatedAt();
            String newStatus = entity.getStatus();
            checklistTemplateMapper.updateStatusByOnboardingTemplateId(
                    entity.getOnboardingTemplateId(), newStatus, now);
            taskTemplateMapper.updateStatusByOnboardingTemplateId(
                    entity.getOnboardingTemplateId(), newStatus, now);
        }
        if (request.getChecklists() != null) {
            syncTemplateTree(
                    companyId,
                    entity.getOnboardingTemplateId(),
                    request.getChecklists(),
                    entity.getUpdatedAt());
        }

        OnboardingTemplateResponse response = new OnboardingTemplateResponse();
        response.setTemplateId(entity.getOnboardingTemplateId());
        response.setName(entity.getName());
        response.setStatus(entity.getStatus());
        response.setTemplateKind(entity.getTemplateKind());
        response.setDepartmentTypeCode(entity.getDepartmentTypeCode());
        response.setLevel(entity.getLevel());
        return response;
    }

    private static void validate(BizContext context, OnboardingTemplateUpdateRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getTemplateId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "templateId is required");
        }
        if (request.getName() != null && !StringUtils.hasText(request.getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is required");
        }
        if (request.getStatus() != null && !StringUtils.hasText(request.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "status is required");
        }
    }

    private void syncTemplateTree(
            String companyId,
            String templateId,
            List<ChecklistTemplateUpdateItem> checklists,
            Date now) {
        List<ChecklistTemplateEntity> existingChecklists =
                checklistTemplateMapper.selectByCompanyIdAndOnboardingTemplateId(companyId, templateId);
        Map<String, ChecklistTemplateEntity> checklistById = existingChecklists.stream()
                .collect(Collectors.toMap(
                        ChecklistTemplateEntity::getChecklistTemplateId,
                        c -> c,
                        (a, b) -> a,
                        LinkedHashMap::new));

        List<String> checklistIds = existingChecklists.stream()
                .map(ChecklistTemplateEntity::getChecklistTemplateId)
                .toList();
        List<TaskTemplateEntity> existingTasks = taskTemplateMapper.selectByCompanyIdAndChecklistTemplateIds(
                companyId, checklistIds);
        Map<String, List<TaskTemplateEntity>> tasksByChecklistId = existingTasks.stream()
                .collect(Collectors.groupingBy(
                        TaskTemplateEntity::getChecklistTemplateId,
                        LinkedHashMap::new,
                        Collectors.toList()));
        Map<String, TaskTemplateEntity> taskById = existingTasks.stream()
                .collect(Collectors.toMap(
                        TaskTemplateEntity::getTaskTemplateId,
                        t -> t,
                        (a, b) -> a,
                        LinkedHashMap::new));

        Map<String, List<String>> existingDocIdsByTaskId = taskTemplateRequiredDocumentMapper
                .selectByCompanyIdAndTaskTemplateIds(
                        companyId,
                        existingTasks.stream().map(TaskTemplateEntity::getTaskTemplateId).toList())
                .stream()
                .collect(Collectors.groupingBy(
                        TaskTemplateRequiredDocumentEntity::getTaskTemplateId,
                        LinkedHashMap::new,
                        Collectors.mapping(TaskTemplateRequiredDocumentEntity::getDocumentId, Collectors.toList())));
        Map<String, List<String>> existingResponsibleDepartmentIdsByTaskId = taskTemplateDepartmentCheckpointMapper
                .selectByCompanyIdAndTaskTemplateIds(
                        companyId,
                        existingTasks.stream().map(TaskTemplateEntity::getTaskTemplateId).toList())
                .stream()
                .collect(Collectors.groupingBy(
                        TaskTemplateDepartmentCheckpointEntity::getTaskTemplateId,
                        LinkedHashMap::new,
                        Collectors.mapping(TaskTemplateDepartmentCheckpointEntity::getDepartmentId, Collectors.toList())));

        Set<String> retainedChecklistIds = new LinkedHashSet<>();
        List<ChecklistTemplateUpdateItem> normalizedChecklists = checklists == null ? List.of() : checklists;
        int checklistSort = 0;
        for (ChecklistTemplateUpdateItem checklistItem : normalizedChecklists) {
            if (checklistItem == null) {
                checklistSort++;
                continue;
            }
            String checklistId = trimToNull(checklistItem.getChecklistTemplateId());
            ChecklistTemplateEntity checklistEntity;
            boolean creatingChecklist = !StringUtils.hasText(checklistId);
            if (creatingChecklist) {
                checklistEntity = new ChecklistTemplateEntity();
                checklistId = UuidGenerator.generate();
                checklistEntity.setChecklistTemplateId(checklistId);
                checklistEntity.setCompanyId(companyId);
                checklistEntity.setOnboardingTemplateId(templateId);
                checklistEntity.setCreatedAt(now);
            } else {
                checklistEntity = checklistById.get(checklistId);
                if (checklistEntity == null) {
                    throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid checklistTemplateId: " + checklistId);
                }
            }

            String checklistName = trimToNull(checklistItem.getName());
            if (creatingChecklist && !StringUtils.hasText(checklistName)) {
                checklistName = "Checklist";
            }
            if (checklistName != null) {
                checklistEntity.setName(checklistName);
            }
            if (checklistItem.getStage() != null) {
                checklistEntity.setStage(trimToNull(checklistItem.getStage()));
            }
            if (checklistItem.getDeadlineDays() != null) {
                checklistEntity.setDeadlineDays(checklistItem.getDeadlineDays());
            }
            checklistEntity.setSortOrder(
                    checklistItem.getSortOrder() != null
                            ? checklistItem.getSortOrder()
                            : (creatingChecklist ? checklistSort : checklistEntity.getSortOrder()));
            if (checklistItem.getStatus() != null) {
                String status = trimToNull(checklistItem.getStatus());
                if (status == null) {
                    throw AppException.of(ErrorCodes.BAD_REQUEST, "checklist status is required");
                }
                checklistEntity.setStatus(status);
            } else if (creatingChecklist && !StringUtils.hasText(checklistEntity.getStatus())) {
                checklistEntity.setStatus("ACTIVE");
            }
            checklistEntity.setUpdatedAt(now);

            if (creatingChecklist) {
                if (checklistTemplateMapper.insert(checklistEntity) != 1) {
                    throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create checklist template failed");
                }
            } else {
                if (checklistTemplateMapper.updateByPrimaryKey(checklistEntity) != 1) {
                    throw AppException.of(ErrorCodes.INTERNAL_ERROR, "update checklist template failed");
                }
            }

            retainedChecklistIds.add(checklistId);
            syncTasksForChecklist(
                    companyId,
                    checklistId,
                    checklistItem.getTasks(),
                    tasksByChecklistId.getOrDefault(checklistId, List.of()),
                    taskById,
                    existingDocIdsByTaskId,
                    existingResponsibleDepartmentIdsByTaskId,
                    now);
            checklistSort++;
        }

        for (ChecklistTemplateEntity existingChecklist : existingChecklists) {
            if (!retainedChecklistIds.contains(existingChecklist.getChecklistTemplateId())) {
                deleteChecklistTree(
                        companyId,
                        existingChecklist.getChecklistTemplateId(),
                        tasksByChecklistId.getOrDefault(existingChecklist.getChecklistTemplateId(), List.of()));
            }
        }
    }

    private void syncTasksForChecklist(
            String companyId,
            String checklistId,
            List<TaskTemplateUpdateItem> tasks,
            List<TaskTemplateEntity> existingTasks,
            Map<String, TaskTemplateEntity> taskById,
            Map<String, List<String>> existingDocIdsByTaskId,
            Map<String, List<String>> existingResponsibleDepartmentIdsByTaskId,
            Date now) {
        Map<String, TaskTemplateEntity> existingTaskById = existingTasks.stream()
                .collect(Collectors.toMap(
                        TaskTemplateEntity::getTaskTemplateId,
                        t -> t,
                        (a, b) -> a,
                        LinkedHashMap::new));

        Set<String> retainedTaskIds = new LinkedHashSet<>();
        List<TaskTemplateUpdateItem> normalizedTasks = tasks == null ? List.of() : tasks;
        int taskSort = 0;
        for (TaskTemplateUpdateItem taskItem : normalizedTasks) {
            if (taskItem == null) {
                taskSort++;
                continue;
            }

            String taskTemplateId = trimToNull(taskItem.getTaskTemplateId());
            TaskTemplateEntity taskEntity;
            boolean creatingTask = !StringUtils.hasText(taskTemplateId);
            if (creatingTask) {
                taskEntity = new TaskTemplateEntity();
                taskTemplateId = UuidGenerator.generate();
                taskEntity.setTaskTemplateId(taskTemplateId);
                taskEntity.setCompanyId(companyId);
                taskEntity.setChecklistTemplateId(checklistId);
                taskEntity.setCreatedAt(now);
            } else {
                taskEntity = existingTaskById.get(taskTemplateId);
                if (taskEntity == null) {
                    TaskTemplateEntity existingTask = taskById.get(taskTemplateId);
                    if (existingTask != null) {
                        throw AppException.of(
                                ErrorCodes.BAD_REQUEST,
                                "taskTemplateId does not belong to checklist: " + taskTemplateId);
                    }
                    throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid taskTemplateId: " + taskTemplateId);
                }
            }

            String title = trimToNull(taskItem.getTitle());
            if (!StringUtils.hasText(title) && creatingTask) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "task title is required");
            }
            if (taskItem.getTitle() != null && !StringUtils.hasText(taskItem.getTitle())) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "task title is required");
            }
            if (title != null) {
                taskEntity.setTitle(title);
            }
            if (taskItem.getDescription() != null) {
                taskEntity.setDescription(taskItem.getDescription());
            }
            if (taskItem.getOwnerType() != null) {
                taskEntity.setOwnerType(trimToNull(taskItem.getOwnerType()));
            }
            if (taskItem.getOwnerRefId() != null || taskItem.getResponsibleDepartmentId() != null) {
                taskEntity.setOwnerRefId(resolveOwnerRefId(taskItem));
            }
            if (taskItem.getDueDaysOffset() != null) {
                taskEntity.setDueDaysOffset(taskItem.getDueDaysOffset());
            }
            if (taskItem.getRequireAck() != null) {
                taskEntity.setRequireAck(taskItem.getRequireAck());
            } else if (creatingTask && taskEntity.getRequireAck() == null) {
                taskEntity.setRequireAck(Boolean.FALSE);
            }
            if (taskItem.getRequireDoc() != null) {
                taskEntity.setRequireDoc(taskItem.getRequireDoc());
            } else if (creatingTask && taskEntity.getRequireDoc() == null) {
                taskEntity.setRequireDoc(Boolean.FALSE);
            }
            if (taskItem.getRequiresManagerApproval() != null) {
                taskEntity.setRequiresManagerApproval(taskItem.getRequiresManagerApproval());
            } else if (creatingTask && taskEntity.getRequiresManagerApproval() == null) {
                taskEntity.setRequiresManagerApproval(Boolean.FALSE);
            }
            if (taskItem.getApproverUserId() != null) {
                taskEntity.setApproverUserId(trimToNull(taskItem.getApproverUserId()));
            }
            if ("DEPARTMENT".equalsIgnoreCase(trimToNull(taskEntity.getOwnerType()))
                    && !StringUtils.hasText(taskEntity.getOwnerRefId())) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "responsibleDepartmentId is required when ownerType=DEPARTMENT");
            }
            taskEntity.setSortOrder(
                    taskItem.getSortOrder() != null
                            ? taskItem.getSortOrder()
                            : (creatingTask ? taskSort : taskEntity.getSortOrder()));
            if (taskItem.getStatus() != null) {
                String status = trimToNull(taskItem.getStatus());
                if (status == null) {
                    throw AppException.of(ErrorCodes.BAD_REQUEST, "task status is required");
                }
                taskEntity.setStatus(status);
            } else if (creatingTask && !StringUtils.hasText(taskEntity.getStatus())) {
                taskEntity.setStatus("ACTIVE");
            }
            taskEntity.setUpdatedAt(now);

            List<String> requiredDocumentIds =
                    resolveRequiredDocumentIds(taskItem, taskTemplateId, existingDocIdsByTaskId);
            List<String> responsibleDepartmentIds = resolveResponsibleDepartmentIds(
                    taskItem, taskTemplateId, existingResponsibleDepartmentIdsByTaskId);
            assertRequiredDocumentConfig(
                    companyId,
                    taskEntity.getTitle(),
                    Boolean.TRUE.equals(taskEntity.getRequireAck()),
                    requiredDocumentIds);
            assertResponsibleDepartments(companyId, responsibleDepartmentIds);

            if (creatingTask) {
                if (taskTemplateMapper.insert(taskEntity) != 1) {
                    throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create task template failed");
                }
            } else {
                if (taskTemplateMapper.updateByPrimaryKey(taskEntity) != 1) {
                    throw AppException.of(ErrorCodes.INTERNAL_ERROR, "update task template failed");
                }
            }

            taskTemplateRequiredDocumentMapper.deleteByCompanyIdAndTaskTemplateId(companyId, taskTemplateId);
            for (String documentId : requiredDocumentIds) {
                TaskTemplateRequiredDocumentEntity link = new TaskTemplateRequiredDocumentEntity();
                link.setTaskTemplateRequiredDocumentId(UuidGenerator.generate());
                link.setCompanyId(companyId);
                link.setTaskTemplateId(taskTemplateId);
                link.setDocumentId(documentId);
                link.setCreatedAt(now);
                if (taskTemplateRequiredDocumentMapper.insert(link) != 1) {
                    throw AppException.of(ErrorCodes.INTERNAL_ERROR, "attach required document to task template failed");
                }
            }
            taskTemplateDepartmentCheckpointMapper.deleteByCompanyIdAndTaskTemplateId(companyId, taskTemplateId);
            int checkpointSort = 0;
            for (String departmentId : responsibleDepartmentIds) {
                TaskTemplateDepartmentCheckpointEntity checkpoint = new TaskTemplateDepartmentCheckpointEntity();
                checkpoint.setTaskTemplateDepartmentCheckpointId(UuidGenerator.generate());
                checkpoint.setCompanyId(companyId);
                checkpoint.setTaskTemplateId(taskTemplateId);
                checkpoint.setDepartmentId(departmentId);
                checkpoint.setRequireEvidence(Boolean.TRUE);
                checkpoint.setSortOrder(checkpointSort++);
                checkpoint.setStatus("ACTIVE");
                checkpoint.setCreatedAt(now);
                checkpoint.setUpdatedAt(now);
                if (taskTemplateDepartmentCheckpointMapper.insert(checkpoint) != 1) {
                    throw AppException.of(
                            ErrorCodes.INTERNAL_ERROR,
                            "attach responsible department to task template failed");
                }
            }

            retainedTaskIds.add(taskTemplateId);
            taskSort++;
        }

        for (TaskTemplateEntity existingTask : existingTasks) {
            if (!retainedTaskIds.contains(existingTask.getTaskTemplateId())) {
                deleteTask(companyId, existingTask.getTaskTemplateId());
            }
        }
    }

    private void deleteChecklistTree(
            String companyId,
            String checklistId,
            List<TaskTemplateEntity> tasksUnderChecklist) {
        for (TaskTemplateEntity task : tasksUnderChecklist) {
            deleteTask(companyId, task.getTaskTemplateId());
        }
        if (checklistTemplateMapper.deleteByPrimaryKey(checklistId) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "delete checklist template failed");
        }
    }

    private void deleteTask(String companyId, String taskTemplateId) {
        taskTemplateDepartmentCheckpointMapper.deleteByCompanyIdAndTaskTemplateId(companyId, taskTemplateId);
        taskTemplateRequiredDocumentMapper.deleteByCompanyIdAndTaskTemplateId(companyId, taskTemplateId);
        if (taskTemplateMapper.deleteByPrimaryKey(taskTemplateId) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "delete task template failed");
        }
    }

    private List<String> resolveRequiredDocumentIds(
            TaskTemplateUpdateItem taskItem,
            String taskTemplateId,
            Map<String, List<String>> existingDocIdsByTaskId) {
        if (taskItem.getRequiredDocumentIds() != null) {
            return normalizeRequiredDocumentIds(taskItem.getRequiredDocumentIds());
        }
        return List.copyOf(existingDocIdsByTaskId.getOrDefault(taskTemplateId, List.of()));
    }

    private List<String> resolveResponsibleDepartmentIds(
            TaskTemplateUpdateItem taskItem,
            String taskTemplateId,
            Map<String, List<String>> existingResponsibleDepartmentIdsByTaskId) {
        if (taskItem.getResponsibleDepartmentIds() != null) {
            return normalizeResponsibleDepartmentIds(taskItem.getResponsibleDepartmentIds());
        }
        return List.copyOf(existingResponsibleDepartmentIdsByTaskId.getOrDefault(taskTemplateId, List.of()));
    }

    private void assertRequiredDocumentConfig(
            String companyId,
            String taskTitle,
            boolean requireAck,
            List<String> requiredDocumentIds) {
        if (requireAck && CollectionUtils.isEmpty(requiredDocumentIds)) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "requiredDocumentIds is required when requireAck=true for task " + taskTitle);
        }
        for (String documentId : requiredDocumentIds) {
            DocumentEntity document = documentMapper.selectByPrimaryKey(documentId);
            if (document == null || !companyId.equals(document.getCompanyId())) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid documentId: " + documentId);
            }
            if (StringUtils.hasText(document.getStatus())
                    && !"ACTIVE".equalsIgnoreCase(document.getStatus().trim())) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "document is not ACTIVE: " + documentId);
            }
        }
    }

    private static List<String> normalizeRequiredDocumentIds(List<String> requiredDocumentIds) {
        if (CollectionUtils.isEmpty(requiredDocumentIds)) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String documentId : requiredDocumentIds) {
            if (StringUtils.hasText(documentId)) {
                normalized.add(documentId.trim());
            }
        }
        return List.copyOf(normalized);
    }

    private static List<String> normalizeResponsibleDepartmentIds(List<String> responsibleDepartmentIds) {
        if (CollectionUtils.isEmpty(responsibleDepartmentIds)) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String departmentId : responsibleDepartmentIds) {
            if (StringUtils.hasText(departmentId)) {
                normalized.add(departmentId.trim());
            }
        }
        return List.copyOf(normalized);
    }

    private void assertResponsibleDepartments(String companyId, List<String> departmentIds) {
        for (String departmentId : departmentIds) {
            DepartmentEntity department = departmentMapper.selectByPrimaryKey(departmentId);
            if (department == null || !companyId.equals(department.getCompanyId())) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid responsibleDepartmentId: " + departmentId);
            }
            if (!StringUtils.hasText(department.getManagerUserId())) {
                throw AppException.of(
                        ErrorCodes.BAD_REQUEST,
                        "department must have manager before using as responsibleDepartmentId: " + departmentId);
            }
        }
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String resolveOwnerRefId(TaskTemplateUpdateItem taskItem) {
        String ownerType = StringUtils.hasText(taskItem.getOwnerType())
                ? taskItem.getOwnerType().trim().toUpperCase(Locale.US)
                : null;
        if ("DEPARTMENT".equals(ownerType) && StringUtils.hasText(taskItem.getResponsibleDepartmentId())) {
            return taskItem.getResponsibleDepartmentId().trim();
        }
        if ("DEPARTMENT".equals(ownerType) && !StringUtils.hasText(taskItem.getOwnerRefId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "responsibleDepartmentId is required when ownerType=DEPARTMENT");
        }
        return trimToNull(taskItem.getOwnerRefId());
    }
}
