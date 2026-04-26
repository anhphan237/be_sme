package com.sme.be_sme.modules.onboarding.processor;

import com.sme.be_sme.modules.onboarding.context.OnboardingInstanceCreateContext;
import com.sme.be_sme.modules.onboarding.support.OnboardingInstanceStartDates;
import com.sme.be_sme.modules.company.infrastructure.mapper.DepartmentMapper;
import com.sme.be_sme.modules.company.infrastructure.persistence.entity.DepartmentEntity;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapper;
import com.sme.be_sme.modules.employee.infrastructure.mapper.EmployeeProfileMapperExt;
import com.sme.be_sme.modules.employee.infrastructure.persistence.entity.EmployeeProfileEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * Creates only the OnboardingInstance record (DRAFT). Checklist and task instances
 * are created later by com.sme.onboarding.task.generate (triggered after activate).
 */
@Component
@RequiredArgsConstructor
public class OnboardingInstanceCreateCloneCoreProcessor
        extends BaseCoreProcessor<OnboardingInstanceCreateContext> {

    private final OnboardingInstanceMapper onboardingInstanceMapper;
    private final EmployeeProfileMapper employeeProfileMapper;
    private final EmployeeProfileMapperExt employeeProfileMapperExt;
    private final DepartmentMapper departmentMapper;

    @Override
    protected Object process(OnboardingInstanceCreateContext ctx) {
        if (ctx.getExistingInstance() != null) {
            return null;
        }
        String companyId = ctx.getBiz().getTenantId();
        String operatorId = ctx.getBiz().getOperatorId();
        Date now = new Date();

        String onboardingId = UuidGenerator.generate();
        ctx.setInstanceId(onboardingId);

        OnboardingInstanceEntity inst = new OnboardingInstanceEntity();
        inst.setOnboardingId(onboardingId);
        inst.setCompanyId(companyId);
        inst.setEmployeeId(ctx.getRequest().getEmployeeId());
        inst.setOnboardingTemplateId(ctx.getTemplate().getOnboardingTemplateId());
        inst.setStatus("DRAFT");
        inst.setCreatedBy(operatorId);
        inst.setCreatedAt(now);
        inst.setUpdatedAt(now);
        inst.setProgressPercent(0);
        inst.setRequestNo(ctx.getRequest().getRequestNo());
        String managerUserId = resolveManagerUserId(companyId, ctx.getRequest().getEmployeeId(), ctx.getRequest().getManagerId());
        if (StringUtils.hasText(managerUserId)) {
            inst.setManagerUserId(managerUserId);
        }
        if (ctx.getRequest().getItStaffUserId() != null && !ctx.getRequest().getItStaffUserId().isBlank()) {
            inst.setItStaffUserId(ctx.getRequest().getItStaffUserId().trim());
        }
        inst.setStartDate(OnboardingInstanceStartDates.resolveExpectedOrToday(ctx.getRequest().getStartDate()));
        onboardingInstanceMapper.insert(inst);
        return null;
    }

    private String resolveManagerUserId(String companyId, String employeeIdRaw, String requestManagerId) {
        if (StringUtils.hasText(requestManagerId)) {
            return requestManagerId.trim();
        }
        if (!StringUtils.hasText(employeeIdRaw)) {
            return null;
        }

        String employeeId = employeeIdRaw.trim();
        EmployeeProfileEntity profile = employeeProfileMapper.selectByPrimaryKey(employeeId);
        if (profile == null) {
            profile = employeeProfileMapperExt.selectByCompanyIdAndUserId(companyId, employeeId);
        }
        if (profile == null) {
            return null;
        }
        if (StringUtils.hasText(profile.getManagerUserId())) {
            return profile.getManagerUserId().trim();
        }
        if (!StringUtils.hasText(profile.getDepartmentId())) {
            return null;
        }
        DepartmentEntity department = departmentMapper.selectByPrimaryKey(profile.getDepartmentId().trim());
        if (department == null || !companyId.equals(department.getCompanyId())) {
            return null;
        }
        return StringUtils.hasText(department.getManagerUserId()) ? department.getManagerUserId().trim() : null;
    }
}
