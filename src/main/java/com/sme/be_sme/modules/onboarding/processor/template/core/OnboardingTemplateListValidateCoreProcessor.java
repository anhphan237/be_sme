package com.sme.be_sme.modules.onboarding.processor.template.core;

import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateListContext;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OnboardingTemplateListValidateCoreProcessor extends BaseCoreProcessor<OnboardingTemplateListContext> {

    private static final String LEVEL_TENANT = "TENANT";
    private static final String LEVEL_PLATFORM = "PLATFORM";

    @Override
    protected Object process(OnboardingTemplateListContext ctx) {
        if (ctx.getRequest() == null) {
            ctx.setRequest(new com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateListRequest());
        }
        if (!StringUtils.hasText(ctx.getRequest().getStatus())) {
            ctx.getRequest().setStatus("ACTIVE");
        }
        if (StringUtils.hasText(ctx.getRequest().getLevel())) {
            String level = ctx.getRequest().getLevel().trim().toUpperCase();
            if (!LEVEL_TENANT.equals(level) && !LEVEL_PLATFORM.equals(level)) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "level must be TENANT or PLATFORM");
            }
            ctx.getRequest().setLevel(level);
        }
        return null;
    }
}
