package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskGenerateRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskGenerationResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.*;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.*;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskApprovalAuthority;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskWorkflow;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OnboardingTaskGenerateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final ChecklistTemplateMapper checklistTemplateMapper;
    private final ChecklistInstanceMapper checklistInstanceMapper;
    private final TaskTemplateMapper taskTemplateMapper;
    private final TaskTemplateDepartmentCheckpointMapper taskTemplateDepartmentCheckpointMapper;
    private final TaskTemplateRequiredDocumentMapper taskTemplateRequiredDocumentMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final TaskDepartmentCheckpointMapper taskDepartmentCheckpointMapper;
    private final TaskRequiredDocumentMapper taskRequiredDocumentMapper;
    private final TaskInstanceMapperExt taskInstanceMapperExt;
    private final EmployeeProfileMapperExt employeeProfileMapperExt;
    private final OnboardingTaskApprovalAuthority approvalAuthority;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingTaskGenerateRequest request = objectMapper.convertValue(payload, OnboardingTaskGenerateRequest.class);
        validate(context, request);

        OnboardingInstanceEntity instance = onboardingInstanceMapper.selectByPrimaryKey(request.getInstanceId().trim());
        if (instance == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "onboarding instance not found");
        }
        if (!context.getTenantId().equals(instance.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "instance does not belong to tenant");
        }

        String companyId = context.getTenantId();
        String reporterUserId = StringUtils.hasText(context.getOperatorId())
                ? context.getOperatorId().trim()
                : (StringUtils.hasText(instance.getCreatedBy()) ? instance.getCreatedBy().trim() : "system");
        List<ChecklistInstanceEntity> existingChecklists =
                checklistInstanceMapper.selectByCompanyIdAndOnboardingId(companyId, instance.getOnboardingId());
        List<TaskInstanceEntity> existingTasks = taskInstanceMapper.selectByCompanyIdAndOnboardingId(
                companyId, instance.getOnboardingId());
        int existingTaskCount = existingTasks == null ? 0 : existingTasks.size();

        if (existingChecklists != null && !existingChecklists.isEmpty() && existingTaskCount > 0) {
            activateIfDraft(instance, context.getOperatorId());
            OnboardingTaskGenerationResponse skip = new OnboardingTaskGenerationResponse();
            skip.setInstanceId(instance.getOnboardingId());
            skip.setTotalTasks(existingTaskCount);
            skip.setAlreadyGenerated(true);
            return skip;
        }

        if (existingChecklists != null && !existingChecklists.isEmpty() && existingTaskCount == 0) {
            removeFailedGenerationShells(companyId, existingChecklists);
        }

        OnboardingTaskGenerateRequest effective = mergeGenerateRequest(companyId, instance, request);

        List<ChecklistTemplateEntity> checklistTemplates = filterChecklistTemplates(companyId, instance.getOnboardingTemplateId());
        if (checklistTemplates.isEmpty()) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "no checklist templates for onboarding_template_id="
                            + instance.getOnboardingTemplateId()
                            + "; cannot create tasks");
        }
        List<TaskTemplateEntity> taskTemplates = filterTaskTemplates(companyId);
        Map<String, List<String>> requiredDocumentIdsByTaskTemplateId =
                loadRequiredDocumentIdsByTaskTemplate(companyId, taskTemplates);
        Map<String, List<String>> responsibleDepartmentIdsByTaskTemplateId =
                loadResponsibleDepartmentIdsByTaskTemplate(companyId, taskTemplates);

        String lineManagerResolved = approvalAuthority.resolveLineManagerUserId(companyId, instance);

        Date now = new Date();
        Date scheduleBaseDate = instance.getStartDate() != null ? instance.getStartDate() : now;
        Map<String, List<TaskTemplateEntity>> tasksByChecklistTemplateId = taskTemplates.stream()
                .filter(t -> StringUtils.hasText(t.getChecklistTemplateId()))
                .collect(Collectors.groupingBy(
                        t -> t.getChecklistTemplateId().trim(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        List<ChecklistTemplateEntity> orderedChecklistTemplates = checklistTemplates.stream()
                .sorted(Comparator.comparingInt(this::sortOrderOrDefault))
                .toList();

        int previousStageEndOffset = 0;
        int totalTasks = 0;
        for (ChecklistTemplateEntity checklistTemplate : orderedChecklistTemplates) {
            String checklistId = UuidGenerator.generate();
            ChecklistInstanceEntity checklistInstance = new ChecklistInstanceEntity();
            checklistInstance.setChecklistId(checklistId);
            checklistInstance.setCompanyId(companyId);
            checklistInstance.setOnboardingId(instance.getOnboardingId());
            checklistInstance.setName(checklistTemplate.getName());
            checklistInstance.setStage(checklistTemplate.getStage());
            checklistInstance.setStatus("NOT_STARTED");
            checklistInstance.setProgressPercent(0);
            int stageEndOffset = Math.max(
                    previousStageEndOffset,
                    resolveStageEndOffset(
                            checklistTemplate,
                            tasksByChecklistTemplateId.getOrDefault(checklistTemplate.getChecklistTemplateId(), List.of())));
            checklistInstance.setOpenAt(calculateDueDate(scheduleBaseDate, previousStageEndOffset));
            checklistInstance.setDeadlineAt(calculateDueDate(scheduleBaseDate, stageEndOffset));
            checklistInstance.setCreatedAt(now);
            checklistInstance.setUpdatedAt(now);

            int insertedChecklist = checklistInstanceMapper.insert(checklistInstance);
            if (insertedChecklist != 1) {
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create checklist instance failed");
            }

            for (TaskTemplateEntity taskTemplate
                    : tasksByChecklistTemplateId.getOrDefault(checklistTemplate.getChecklistTemplateId(), List.of())) {
                if (Boolean.TRUE.equals(taskTemplate.getRequiresManagerApproval())) {
                    boolean designated = StringUtils.hasText(taskTemplate.getApproverUserId());
                    if (!designated && !StringUtils.hasText(lineManagerResolved)) {
                        throw AppException.of(
                                ErrorCodes.BAD_REQUEST,
                                "manager approval is required for task template "
                                        + taskTemplate.getTaskTemplateId()
                                        + ": set approverUserId on the template or configure the employee line manager");
                    }
                }
                TaskInstanceEntity taskInstance = new TaskInstanceEntity();
                taskInstance.setTaskId(UuidGenerator.generate());
                taskInstance.setCompanyId(companyId);
                taskInstance.setChecklistId(checklistId);
                taskInstance.setTaskTemplateId(taskTemplate.getTaskTemplateId());
                taskInstance.setTitle(taskTemplate.getTitle());
                taskInstance.setDescription(taskTemplate.getDescription());
                taskInstance.setStatus(OnboardingTaskWorkflow.STATUS_TODO);
                taskInstance.setDueDate(calculateDueDate(scheduleBaseDate, taskTemplate.getDueDaysOffset()));
                applyOwnerAssignment(taskInstance, taskTemplate, instance, effective);
                taskInstance.setRequireAck(Boolean.TRUE.equals(taskTemplate.getRequireAck()));
                taskInstance.setRequireDoc(Boolean.TRUE.equals(taskTemplate.getRequireDoc()));
                taskInstance.setRequiresManagerApproval(Boolean.TRUE.equals(taskTemplate.getRequiresManagerApproval()));
                taskInstance.setApproverUserId(
                        StringUtils.hasText(taskTemplate.getApproverUserId())
                                ? taskTemplate.getApproverUserId().trim()
                                : null);
                taskInstance.setApprovalStatus(OnboardingTaskWorkflow.APPROVAL_NONE);
                taskInstance.setScheduleStatus(OnboardingTaskWorkflow.SCHEDULE_UNSCHEDULED);
                taskInstance.setCreatedBy(reporterUserId);
                taskInstance.setCreatedAt(now);
                taskInstance.setUpdatedAt(now);

                int insertedTask = taskInstanceMapper.insert(taskInstance);
                if (insertedTask != 1) {
                    throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create task instance failed");
                }
                List<String> requiredDocumentIds = requiredDocumentIdsByTaskTemplateId
                        .getOrDefault(taskTemplate.getTaskTemplateId(), List.of());
                List<String> responsibleDepartmentIds = responsibleDepartmentIdsByTaskTemplateId
                        .getOrDefault(taskTemplate.getTaskTemplateId(), List.of());
                for (String documentId : requiredDocumentIds) {
                    TaskRequiredDocumentEntity requiredDocument = new TaskRequiredDocumentEntity();
                    requiredDocument.setTaskRequiredDocumentId(UuidGenerator.generate());
                    requiredDocument.setCompanyId(companyId);
                    requiredDocument.setTaskId(taskInstance.getTaskId());
                    requiredDocument.setDocumentId(documentId);
                    requiredDocument.setCreatedAt(now);
                    if (taskRequiredDocumentMapper.insert(requiredDocument) != 1) {
                        throw AppException.of(ErrorCodes.INTERNAL_ERROR, "attach required document to task failed");
                    }
                }
                for (String departmentId : responsibleDepartmentIds) {
                    TaskDepartmentCheckpointEntity checkpoint = new TaskDepartmentCheckpointEntity();
                    checkpoint.setTaskDepartmentCheckpointId(UuidGenerator.generate());
                    checkpoint.setCompanyId(companyId);
                    checkpoint.setTaskId(taskInstance.getTaskId());
                    checkpoint.setDepartmentId(departmentId);
                    checkpoint.setStatus("PENDING");
                    checkpoint.setRequireEvidence(Boolean.TRUE);
                    checkpoint.setCreatedAt(now);
                    checkpoint.setUpdatedAt(now);
                    if (taskDepartmentCheckpointMapper.insert(checkpoint) != 1) {
                        throw AppException.of(
                                ErrorCodes.INTERNAL_ERROR,
                                "attach responsible department checkpoint to task failed");
                    }
                }
                totalTasks++;
            }
            previousStageEndOffset = stageEndOffset;
        }

        if (totalTasks == 0) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "no task instances were created: ensure task_templates reference the correct checklist_template_id "
                            + "for this onboarding template");
        }

        OnboardingTaskGenerationResponse response = new OnboardingTaskGenerationResponse();
        activateIfDraft(instance, context.getOperatorId());
        response.setInstanceId(request.getInstanceId());
        response.setTotalTasks(totalTasks);
        response.setAlreadyGenerated(false);
        return response;
    }

    private void activateIfDraft(OnboardingInstanceEntity instance, String operatorId) {
        if (instance == null || !StringUtils.hasText(instance.getStatus())) {
            return;
        }
        if (!"DRAFT".equalsIgnoreCase(instance.getStatus())) {
            return;
        }

        instance.setStatus("ACTIVE");
        instance.setUpdatedAt(new Date());
        if (StringUtils.hasText(operatorId)) {
            instance.setUpdatedBy(operatorId.trim());
        }

        int updated = onboardingInstanceMapper.updateByPrimaryKey(instance);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "activate onboarding instance failed");
        }
    }

    private void removeFailedGenerationShells(String companyId, List<ChecklistInstanceEntity> checklists) {
        for (ChecklistInstanceEntity ch : checklists) {
            if (ch == null || !StringUtils.hasText(ch.getChecklistId())) {
                continue;
            }
            taskInstanceMapperExt.deleteByCompanyIdAndChecklistId(companyId, ch.getChecklistId().trim());
            int deleted = checklistInstanceMapper.deleteByPrimaryKey(ch.getChecklistId().trim());
            if (deleted != 1) {
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "remove orphan checklist instance failed");
            }
        }
    }

    private OnboardingTaskGenerateRequest mergeGenerateRequest(
            String companyId, OnboardingInstanceEntity instance, OnboardingTaskGenerateRequest request) {
        OnboardingTaskGenerateRequest effective = new OnboardingTaskGenerateRequest();
        effective.setInstanceId(request.getInstanceId());

        String managerId = StringUtils.hasText(request.getManagerId()) ? request.getManagerId().trim() : null;
        if (!StringUtils.hasText(managerId) && StringUtils.hasText(instance.getManagerUserId())) {
            managerId = instance.getManagerUserId().trim();
        }
        if (!StringUtils.hasText(managerId)) {
            managerId = approvalAuthority.resolveLineManagerUserId(companyId, instance);
        }
        effective.setManagerId(managerId);

        String itId = StringUtils.hasText(request.getItStaffUserId()) ? request.getItStaffUserId().trim() : null;
        if (!StringUtils.hasText(itId) && StringUtils.hasText(instance.getItStaffUserId())) {
            itId = instance.getItStaffUserId().trim();
        }
        effective.setItStaffUserId(itId);
        return effective;
    }

    private static void validate(BizContext context, OnboardingTaskGenerateRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getInstanceId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "instanceId is required");
        }
    }

    private List<ChecklistTemplateEntity> filterChecklistTemplates(String tenantId, String onboardingTemplateId) {
        if (!StringUtils.hasText(onboardingTemplateId)) {
            return List.of();
        }
        List<ChecklistTemplateEntity> templates =
                checklistTemplateMapper.selectByCompanyIdAndOnboardingTemplateId(tenantId, onboardingTemplateId);
        return templates == null ? List.of() : templates;
    }

    private List<TaskTemplateEntity> filterTaskTemplates(String tenantId) {
        List<TaskTemplateEntity> templates = taskTemplateMapper.selectByCompanyId(tenantId);
        return templates == null ? List.of() : templates;
    }

    private Map<String, List<String>> loadRequiredDocumentIdsByTaskTemplate(
            String companyId,
            List<TaskTemplateEntity> taskTemplates) {
        if (taskTemplates == null || taskTemplates.isEmpty()) {
            return Map.of();
        }
        List<String> taskTemplateIds = taskTemplates.stream()
                .map(TaskTemplateEntity::getTaskTemplateId)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        if (taskTemplateIds.isEmpty()) {
            return Map.of();
        }
        return taskTemplateRequiredDocumentMapper
                .selectByCompanyIdAndTaskTemplateIds(companyId, taskTemplateIds)
                .stream()
                .collect(
                        java.util.stream.Collectors.groupingBy(
                                TaskTemplateRequiredDocumentEntity::getTaskTemplateId,
                                java.util.stream.Collectors.mapping(
                                        TaskTemplateRequiredDocumentEntity::getDocumentId,
                                        java.util.stream.Collectors.toList())));
    }

    private Map<String, List<String>> loadResponsibleDepartmentIdsByTaskTemplate(
            String companyId,
            List<TaskTemplateEntity> taskTemplates) {
        if (taskTemplates == null || taskTemplates.isEmpty()) {
            return Map.of();
        }
        List<String> taskTemplateIds = taskTemplates.stream()
                .map(TaskTemplateEntity::getTaskTemplateId)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        if (taskTemplateIds.isEmpty()) {
            return Map.of();
        }
        return taskTemplateDepartmentCheckpointMapper
                .selectByCompanyIdAndTaskTemplateIds(companyId, taskTemplateIds)
                .stream()
                .collect(Collectors.groupingBy(
                        TaskTemplateDepartmentCheckpointEntity::getTaskTemplateId,
                        Collectors.mapping(TaskTemplateDepartmentCheckpointEntity::getDepartmentId, Collectors.toList())));
    }

    private static Date calculateDueDate(Date start, Integer daysOffset) {
        if (start == null) {
            return null;
        }
        if (daysOffset == null) {
            return start;
        }
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.setTime(start);
        calendar.add(Calendar.DATE, daysOffset);
        return calendar.getTime();
    }

    private int resolveStageEndOffset(ChecklistTemplateEntity checklistTemplate, List<TaskTemplateEntity> stageTasks) {
        if (checklistTemplate != null && checklistTemplate.getDeadlineDays() != null) {
            return checklistTemplate.getDeadlineDays();
        }
        return stageTasks.stream()
                .map(TaskTemplateEntity::getDueDaysOffset)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
    }

    private int sortOrderOrDefault(ChecklistTemplateEntity checklistTemplate) {
        if (checklistTemplate == null || checklistTemplate.getSortOrder() == null) {
            return Integer.MAX_VALUE;
        }
        return checklistTemplate.getSortOrder();
    }

    private void applyOwnerAssignment(TaskInstanceEntity taskInstance, TaskTemplateEntity template,
                                      OnboardingInstanceEntity instance, OnboardingTaskGenerateRequest request) {
        if (taskInstance == null || template == null) {
            return;
        }
        if (!StringUtils.hasText(template.getOwnerType())) {
            return;
        }
        String ownerType = template.getOwnerType().trim().toUpperCase(Locale.US);
        if ("USER".equals(ownerType) && StringUtils.hasText(template.getOwnerRefId())) {
            taskInstance.setAssignedUserId(template.getOwnerRefId().trim());
        } else if ("DEPARTMENT".equals(ownerType) && StringUtils.hasText(template.getOwnerRefId())) {
            taskInstance.setAssignedDepartmentId(template.getOwnerRefId().trim());
        } else if ("EMPLOYEE".equals(ownerType) && instance != null && StringUtils.hasText(instance.getEmployeeId())) {
            EmployeeProfileEntity profile = employeeProfileMapperExt.selectByCompanyIdAndUserId(
                    instance.getCompanyId(), instance.getEmployeeId().trim());
            if (profile == null || !StringUtils.hasText(profile.getUserId())) {
                throw AppException.of(
                        ErrorCodes.BAD_REQUEST,
                        "employee profile or user_id missing for EMPLOYEE task owner");
            }
            taskInstance.setAssignedUserId(profile.getUserId().trim());
        } else if ("MANAGER".equals(ownerType) && request != null && StringUtils.hasText(request.getManagerId())) {
            taskInstance.setAssignedUserId(request.getManagerId());
        } else if ("IT_STAFF".equals(ownerType) && request != null && StringUtils.hasText(request.getItStaffUserId())) {
            taskInstance.setAssignedUserId(request.getItStaffUserId());
        }
    }
}
