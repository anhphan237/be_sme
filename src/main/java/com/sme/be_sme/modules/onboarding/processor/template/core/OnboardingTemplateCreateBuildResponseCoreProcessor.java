package com.sme.be_sme.modules.onboarding.processor.template.core;

import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateResponse;
import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateCreateContext;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import org.springframework.stereotype.Component;

@Component
public class OnboardingTemplateCreateBuildResponseCoreProcessor extends BaseCoreProcessor<OnboardingTemplateCreateContext> {

    @Override
    protected Object process(OnboardingTemplateCreateContext ctx) {
        OnboardingTemplateResponse response = new OnboardingTemplateResponse();
        response.setTemplateId(ctx.getTemplateId());
        response.setName(ctx.getTemplateEntity().getName());
        response.setStatus(ctx.getTemplateEntity().getStatus());
        response.setTemplateKind(ctx.getTemplateEntity().getTemplateKind());
        response.setDepartmentTypeCode(ctx.getTemplateEntity().getDepartmentTypeCode());
        response.setLevel(ctx.getTemplateEntity().getLevel());
        ctx.setResponse(response);
        return null;
    }
}
