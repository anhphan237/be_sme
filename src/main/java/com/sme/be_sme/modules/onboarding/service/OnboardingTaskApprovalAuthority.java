package com.sme.be_sme.modules.onboarding.service;

import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapper;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.ChecklistInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.ChecklistInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.TaskInstanceEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Enforces that only the designated approver ({@code task.approverUserId}) or the employee line manager
 * may approve, reject, or direct-DONE a task that requires manager approval.
 */
@Component
@RequiredArgsConstructor
public class OnboardingTaskApprovalAuthority {

    private final ChecklistInstanceMapper checklistInstanceMapper;
    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final EmployeeProfileMapper employeeProfileMapper;
    private final EmployeeProfileMapperExt employeeProfileMapperExt;

    public void assertMayApproveOrRejectOrForceDone(BizContext context, String companyId, TaskInstanceEntity task) {
        if (!Boolean.TRUE.equals(task.getRequiresManagerApproval())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "task does not require manager approval");
        }
        String operatorId = context != null ? context.getOperatorId() : null;
        if (!StringUtils.hasText(operatorId)) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "operator required");
        }
        operatorId = operatorId.trim();

        if (StringUtils.hasText(task.getApproverUserId())) {
            if (!operatorId.equals(task.getApproverUserId().trim())) {
                throw AppException.of(ErrorCodes.FORBIDDEN, "only the designated approver may perform this action");
            }
            return;
        }

        OnboardingInstanceEntity instance = loadOnboardingInstance(companyId, task);
        String lineManagerUserId = resolveLineManagerUserId(companyId, instance);
        if (!StringUtils.hasText(lineManagerUserId)) {
            throw AppException.of(
                    ErrorCodes.FORBIDDEN,
                    "line manager is not configured; set manager on onboarding instance or employee profile, "
                            + "or set approverUserId on the task template");
        }
        if (!operatorId.equals(lineManagerUserId)) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "only the employee line manager may perform this action");
        }
    }

    public OnboardingInstanceEntity loadOnboardingInstance(String companyId, TaskInstanceEntity task) {
        if (task == null || !StringUtils.hasText(task.getChecklistId())) {
            return null;
        }
        ChecklistInstanceEntity checklist = checklistInstanceMapper.selectByPrimaryKey(task.getChecklistId().trim());
        if (checklist == null || !companyId.equals(checklist.getCompanyId())) {
            return null;
        }
        if (!StringUtils.hasText(checklist.getOnboardingId())) {
            return null;
        }
        return onboardingInstanceMapper.selectByPrimaryKey(checklist.getOnboardingId().trim());
    }

    /**
     * Line manager: onboarding instance field first, then employee profile {@code manager_user_id}.
     */
    public String resolveLineManagerUserId(String companyId, OnboardingInstanceEntity instance) {
        if (instance == null) {
            return null;
        }
        if (StringUtils.hasText(instance.getManagerUserId())) {
            return instance.getManagerUserId().trim();
        }
        if (!StringUtils.hasText(instance.getEmployeeId())) {
            return null;
        }
        String key = instance.getEmployeeId().trim();
        EmployeeProfileEntity byEmpId = employeeProfileMapper.selectByPrimaryKey(key);
        if (byEmpId != null && StringUtils.hasText(byEmpId.getManagerUserId())) {
            return byEmpId.getManagerUserId().trim();
        }
        EmployeeProfileEntity byUserId = employeeProfileMapperExt.selectByCompanyIdAndUserId(companyId, key);
        if (byUserId != null && StringUtils.hasText(byUserId.getManagerUserId())) {
            return byUserId.getManagerUserId().trim();
        }
        return null;
    }
}
