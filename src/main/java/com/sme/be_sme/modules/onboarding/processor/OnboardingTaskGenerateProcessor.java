package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapper;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskGenerateRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskGenerationResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.ChecklistInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.ChecklistTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.ChecklistInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.ChecklistTemplateEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskTemplateEntity;
import com.sme.be_sme.modules.onboarding.service.OnboardingTaskApprovalAuthority;
import com.sme.be_sme.modules.onboarding.support.OnboardingTaskWorkflow;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingTaskGenerateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final ChecklistTemplateMapper checklistTemplateMapper;
    private final ChecklistInstanceMapper checklistInstanceMapper;
    private final TaskTemplateMapper taskTemplateMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final EmployeeProfileMapper employeeProfileMapper;
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
        List<ChecklistInstanceEntity> existingChecklists =
                checklistInstanceMapper.selectByCompanyIdAndOnboardingId(companyId, instance.getOnboardingId());
        if (existingChecklists != null && !existingChecklists.isEmpty()) {
            List<TaskInstanceEntity> tasks = taskInstanceMapper.selectByCompanyIdAndOnboardingId(
                    companyId, instance.getOnboardingId());
            int n = tasks == null ? 0 : tasks.size();
            OnboardingTaskGenerationResponse skip = new OnboardingTaskGenerationResponse();
            skip.setInstanceId(instance.getOnboardingId());
            skip.setTotalTasks(n);
            skip.setAlreadyGenerated(true);
            return skip;
        }

        OnboardingTaskGenerateRequest effective = mergeGenerateRequest(companyId, instance, request);

        List<ChecklistTemplateEntity> checklistTemplates = filterChecklistTemplates(companyId, instance.getOnboardingTemplateId());
        List<TaskTemplateEntity> taskTemplates = filterTaskTemplates(companyId);

        String lineManagerResolved = approvalAuthority.resolveLineManagerUserId(companyId, instance);

        Date now = new Date();
        int totalTasks = 0;
        for (ChecklistTemplateEntity checklistTemplate : checklistTemplates) {
            String checklistId = UuidGenerator.generate();
            ChecklistInstanceEntity checklistInstance = new ChecklistInstanceEntity();
            checklistInstance.setChecklistId(checklistId);
            checklistInstance.setCompanyId(companyId);
            checklistInstance.setOnboardingId(instance.getOnboardingId());
            checklistInstance.setName(checklistTemplate.getName());
            checklistInstance.setStage(checklistTemplate.getStage());
            checklistInstance.setStatus("NOT_STARTED");
            checklistInstance.setProgressPercent(0);
            checklistInstance.setCreatedAt(now);
            checklistInstance.setUpdatedAt(now);

            int insertedChecklist = checklistInstanceMapper.insert(checklistInstance);
            if (insertedChecklist != 1) {
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create checklist instance failed");
            }

            for (TaskTemplateEntity taskTemplate : taskTemplates) {
                if (!checklistTemplate.getChecklistTemplateId().equals(taskTemplate.getChecklistTemplateId())) {
                    continue;
                }
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
                taskInstance.setStatus("TODO");
                taskInstance.setDueDate(calculateDueDate(now, taskTemplate.getDueDaysOffset()));
                applyOwnerAssignment(taskInstance, taskTemplate, instance, effective);
                taskInstance.setRequireAck(Boolean.TRUE.equals(taskTemplate.getRequireAck()));
                taskInstance.setRequiresManagerApproval(Boolean.TRUE.equals(taskTemplate.getRequiresManagerApproval()));
                taskInstance.setApproverUserId(
                        StringUtils.hasText(taskTemplate.getApproverUserId())
                                ? taskTemplate.getApproverUserId().trim()
                                : null);
                taskInstance.setApprovalStatus(OnboardingTaskWorkflow.APPROVAL_NONE);
                taskInstance.setCreatedBy("system");
                taskInstance.setCreatedAt(now);
                taskInstance.setUpdatedAt(now);

                int insertedTask = taskInstanceMapper.insert(taskInstance);
                if (insertedTask != 1) {
                    throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create task instance failed");
                }
                totalTasks++;
            }
        }

        OnboardingTaskGenerationResponse response = new OnboardingTaskGenerationResponse();
        response.setInstanceId(request.getInstanceId());
        response.setTotalTasks(totalTasks);
        response.setAlreadyGenerated(false);
        return response;
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
        List<ChecklistTemplateEntity> templates = checklistTemplateMapper.selectAll();
        List<ChecklistTemplateEntity> filtered = new ArrayList<>();
        for (ChecklistTemplateEntity template : templates) {
            if (template == null) {
                continue;
            }
            if (!tenantId.equals(template.getCompanyId())) {
                continue;
            }
            if (!onboardingTemplateId.equals(template.getOnboardingTemplateId())) {
                continue;
            }
            filtered.add(template);
        }
        return filtered;
    }

    private List<TaskTemplateEntity> filterTaskTemplates(String tenantId) {
        List<TaskTemplateEntity> templates = taskTemplateMapper.selectAll();
        List<TaskTemplateEntity> filtered = new ArrayList<>();
        for (TaskTemplateEntity template : templates) {
            if (template == null) {
                continue;
            }
            if (!tenantId.equals(template.getCompanyId())) {
                continue;
            }
            filtered.add(template);
        }
        return filtered;
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
            EmployeeProfileEntity profile =
                    employeeProfileMapper.selectByPrimaryKey(instance.getEmployeeId().trim());
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
