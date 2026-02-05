package com.sme.be_sme.modules.onboarding.processor;

import com.sme.be_sme.modules.onboarding.context.OnboardingInstanceCreateContext;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingInstanceEntity;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceCreateIdempotencyCoreProcessor
        extends BaseCoreProcessor<OnboardingInstanceCreateContext> {

    private final OnboardingInstanceMapper onboardingInstanceMapper;

    @Override
    protected Object process(OnboardingInstanceCreateContext ctx) {
        if (!StringUtils.hasText(ctx.getRequest().getRequestNo())) {
            return null;
        }
        String companyId = ctx.getBiz().getTenantId();
        OnboardingInstanceEntity existing = onboardingInstanceMapper.selectByCompanyIdAndRequestNo(
                companyId, ctx.getRequest().getRequestNo().trim());
        if (existing != null) {
            ctx.setExistingInstance(existing);
        }
        return null;
    }
}
