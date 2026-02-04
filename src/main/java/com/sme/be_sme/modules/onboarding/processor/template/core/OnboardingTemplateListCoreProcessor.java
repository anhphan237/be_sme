package com.sme.be_sme.modules.onboarding.processor.template.core;

import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateListResponse;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateResponse;
import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateListContext;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OnboardingTemplateListCoreProcessor extends BaseCoreProcessor<OnboardingTemplateListContext> {

    private final OnboardingTemplateMapper onboardingTemplateMapper;

    @Override
    protected Object process(OnboardingTemplateListContext ctx) {
        String companyId = ctx.getBiz().getTenantId(); // tenantId = companyId
        String status = ctx.getRequest().getStatus();

        List<OnboardingTemplateEntity> rows =
                onboardingTemplateMapper.selectByCompanyIdAndStatus(companyId, status);

        ctx.setTemplates(rows);

        OnboardingTemplateListResponse res = ctx.getResponse();
        res.setTemplates(rows.stream().map(this::toResponse).toList());

        return null;
    }

    private OnboardingTemplateResponse toResponse(OnboardingTemplateEntity e) {
        OnboardingTemplateResponse r = new OnboardingTemplateResponse();
        r.setTemplateId(e.getOnboardingTemplateId());
        r.setName(e.getName());
        r.setStatus(e.getStatus());
        return r;
    }
}
