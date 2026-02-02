package com.sme.be_sme.modules.onboarding.processor;

import com.sme.be_sme.modules.onboarding.context.OnboardingInstanceCreateContext;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OnboardingInstanceCreateValidateCoreProcessor
        extends BaseCoreProcessor<OnboardingInstanceCreateContext> {

    @Override
    protected Object process(OnboardingInstanceCreateContext ctx) {
        if (ctx.getRequest() == null ||
                !StringUtils.hasText(ctx.getRequest().getTemplateId()) ||
                !StringUtils.hasText(ctx.getRequest().getEmployeeId())) {
            throw AppException.of("INVALID_REQUEST", "templateId & employeeId are required");
        }
        return null;
    }
}

