package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceGetRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceDetailResponse;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.ChecklistInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.TaskInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.ChecklistInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceGetProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final ChecklistInstanceMapper checklistInstanceMapper;
    private final TaskInstanceMapper taskInstanceMapper;
    private final EmployeeProfileMapper employeeProfileMapper;
    private final EmployeeProfileMapperExt employeeProfileMapperExt;
    private final UserMapperExt userMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingInstanceGetRequest request = objectMapper.convertValue(payload, OnboardingInstanceGetRequest.class);
        validate(context, request);

        String companyId = context.getTenantId();
        OnboardingInstanceEntity instance = onboardingInstanceMapper.selectByPrimaryKey(request.getInstanceId().trim());
        if (instance == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "onboarding instance not found");
        }
        if (!companyId.equals(instance.getCompanyId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "instance does not belong to tenant");
        }
        enforceEmployeeOwnInstanceOnly(context, instance);

        OnboardingInstanceDetailResponse response = new OnboardingInstanceDetailResponse();
        response.setInstanceId(instance.getOnboardingId());
        response.setEmployeeId(instance.getEmployeeId());
        EmployeeLinkInfo employeeLinkInfo = resolveEmployeeLinkInfo(companyId, instance.getEmployeeId());
        response.setEmployeeUserId(employeeLinkInfo.employeeUserId);
        response.setManagerUserId(employeeLinkInfo.managerUserId);
        response.setManagerName(employeeLinkInfo.managerName);
        response.setTemplateId(instance.getOnboardingTemplateId());
        response.setStatus(instance.getStatus());
        response.setStartDate(instance.getStartDate());
        response.setCompletedAt(instance.getCompletedAt());
        response.setProgressPercent(instance.getProgressPercent() != null ? instance.getProgressPercent() : 0);

        List<ChecklistInstanceEntity> checklists = checklistInstanceMapper.selectByCompanyIdAndOnboardingId(
                companyId, instance.getOnboardingId());
        List<TaskInstanceEntity> allTasks = taskInstanceMapper.selectByCompanyIdAndOnboardingId(
                companyId, instance.getOnboardingId());

        List<OnboardingInstanceDetailResponse.ChecklistDetailItem> checklistItems = new ArrayList<>();
        if (checklists != null) {
            for (ChecklistInstanceEntity chk : checklists) {
                OnboardingInstanceDetailResponse.ChecklistDetailItem item = new OnboardingInstanceDetailResponse.ChecklistDetailItem();
                item.setChecklistId(chk.getChecklistId());
                item.setName(chk.getName());
                item.setStage(chk.getStage());
                item.setStatus(chk.getStatus());
                item.setProgressPercent(chk.getProgressPercent() != null ? chk.getProgressPercent() : 0);
                List<OnboardingInstanceDetailResponse.TaskDetailItem> taskItems = allTasks == null ? List.of() :
                        allTasks.stream()
                                .filter(t -> chk.getChecklistId().equals(t.getChecklistId()))
                                .map(this::toTaskDetailItem)
                                .collect(Collectors.toList());
                item.setTasks(taskItems);
                checklistItems.add(item);
            }
        }
        response.setChecklists(checklistItems);
        return response;
    }

    private OnboardingInstanceDetailResponse.TaskDetailItem toTaskDetailItem(TaskInstanceEntity t) {
        OnboardingInstanceDetailResponse.TaskDetailItem item = new OnboardingInstanceDetailResponse.TaskDetailItem();
        item.setTaskId(t.getTaskId());
        item.setTitle(t.getTitle());
        item.setStatus(t.getStatus());
        item.setAssignedUserId(t.getAssignedUserId());
        item.setDueDate(t.getDueDate());
        item.setCompletedAt(t.getCompletedAt());
        return item;
    }

    private static void validate(BizContext context, OnboardingInstanceGetRequest request) {
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

    private EmployeeLinkInfo resolveEmployeeLinkInfo(String companyId, String onboardingEmployeeId) {
        if (!StringUtils.hasText(onboardingEmployeeId)) {
            return EmployeeLinkInfo.empty();
        }
        String normalizedId = onboardingEmployeeId.trim();

        EmployeeProfileEntity profileByEmployeeId = employeeProfileMapper.selectByPrimaryKey(normalizedId);
        if (profileByEmployeeId != null && StringUtils.hasText(profileByEmployeeId.getUserId())) {
            return toEmployeeLinkInfo(companyId, profileByEmployeeId);
        }

        EmployeeProfileEntity profileByUserId = employeeProfileMapperExt.selectByCompanyIdAndUserId(companyId, normalizedId);
        if (profileByUserId != null && StringUtils.hasText(profileByUserId.getUserId())) {
            return toEmployeeLinkInfo(companyId, profileByUserId);
        }

        // Compatibility: old onboarding_instances.employee_id may already store identity user_id.
        return new EmployeeLinkInfo(normalizedId, null, null);
    }

    private EmployeeLinkInfo toEmployeeLinkInfo(String companyId, EmployeeProfileEntity profile) {
        String employeeUserId = StringUtils.hasText(profile.getUserId()) ? profile.getUserId().trim() : null;
        String managerUserId = StringUtils.hasText(profile.getManagerUserId()) ? profile.getManagerUserId().trim() : null;
        String managerName = null;
        if (StringUtils.hasText(managerUserId)) {
            UserEntity manager = userMapperExt.selectByCompanyIdAndUserId(companyId, managerUserId);
            managerName = manager != null && StringUtils.hasText(manager.getFullName()) ? manager.getFullName().trim() : null;
        }
        return new EmployeeLinkInfo(employeeUserId, managerUserId, managerName);
    }

    private static class EmployeeLinkInfo {
        private final String employeeUserId;
        private final String managerUserId;
        private final String managerName;

        private EmployeeLinkInfo(String employeeUserId, String managerUserId, String managerName) {
            this.employeeUserId = employeeUserId;
            this.managerUserId = managerUserId;
            this.managerName = managerName;
        }

        private static EmployeeLinkInfo empty() {
            return new EmployeeLinkInfo(null, null, null);
        }
    }
}
