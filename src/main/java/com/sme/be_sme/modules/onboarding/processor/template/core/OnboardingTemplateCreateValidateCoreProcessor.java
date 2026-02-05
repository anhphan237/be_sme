package com.sme.be_sme.modules.onboarding.processor.template.core;

import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateCreateContext;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OnboardingTemplateCreateValidateCoreProcessor extends BaseCoreProcessor<OnboardingTemplateCreateContext> {

    @Override
    protected Object process(OnboardingTemplateCreateContext ctx) {
        if (ctx.getRequest() == null || !StringUtils.hasText(ctx.getRequest().getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is required");
        }
        return null;
    }
}
