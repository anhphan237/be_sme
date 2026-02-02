package com.sme.be_sme.modules.onboarding.processor.template.core;

import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateGetContext;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OnboardingTemplateGetValidateCoreProcessor extends BaseCoreProcessor<OnboardingTemplateGetContext> {

    @Override
    protected Object process(OnboardingTemplateGetContext ctx) {
        if (ctx.getRequest() == null || !StringUtils.hasText(ctx.getRequest().getTemplateId())) {
            throw AppException.of("INVALID_REQUEST", "templateId is required");
        }
        return null;
    }
}