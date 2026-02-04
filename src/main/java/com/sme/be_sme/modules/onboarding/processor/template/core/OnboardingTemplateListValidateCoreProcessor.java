package com.sme.be_sme.modules.onboarding.processor.template.core;

import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateListContext;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OnboardingTemplateListValidateCoreProcessor extends BaseCoreProcessor<OnboardingTemplateListContext> {

    @Override
    protected Object process(OnboardingTemplateListContext ctx) {
        if (ctx.getRequest() == null) {
            ctx.setRequest(new com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateListRequest());
        }
        if (!StringUtils.hasText(ctx.getRequest().getStatus())) {
            ctx.getRequest().setStatus("ACTIVE");
        }
        return null;
    }
}
