package com.sme.be_sme.modules.onboarding.processor;

import com.sme.be_sme.modules.onboarding.context.OnboardingInstanceCreateContext;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import org.springframework.stereotype.Component;

@Component
public class OnboardingInstanceCreateBuildResponseCoreProcessor
        extends BaseCoreProcessor<OnboardingInstanceCreateContext> {

    @Override
    protected Object process(OnboardingInstanceCreateContext ctx) {
        if (ctx.getExistingInstance() != null) {
            OnboardingInstanceEntity e = ctx.getExistingInstance();
            ctx.getResponse().setInstanceId(e.getOnboardingId());
            ctx.getResponse().setStatus(e.getStatus());
            return null;
        }
        ctx.getResponse().setInstanceId(ctx.getInstanceId());
        ctx.getResponse().setStatus("DRAFT");
        return null;
    }
}
