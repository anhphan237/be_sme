package com.sme.be_sme.modules.onboarding.processor;

import com.sme.be_sme.modules.onboarding.context.OnboardingInstanceCreateContext;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
        onboardingInstanceMapper.insert(inst);
        return null;
    }
}
