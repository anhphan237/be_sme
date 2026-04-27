package com.sme.be_sme.modules.onboarding.processor.template.core;

import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateCreateContext;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapperExt;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingTemplateCreateValidateCoreProcessor extends BaseCoreProcessor<OnboardingTemplateCreateContext> {

    private final OnboardingTemplateMapperExt onboardingTemplateMapperExt;

    @Override
    protected Object process(OnboardingTemplateCreateContext ctx) {
        if (ctx.getRequest() == null || !StringUtils.hasText(ctx.getRequest().getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is required");
        }
        if (StringUtils.hasText(ctx.getRequest().getSourceTemplateId())
                && ctx.getRequest().getChecklists() != null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "checklists must be empty when sourceTemplateId is provided");
        }
        if (StringUtils.hasText(ctx.getRequest().getSourceTemplateId())
                && onboardingTemplateMapperExt.existsTenantOnboardingTemplateByName(
                        ctx.getCompanyId(),
                        ctx.getRequest().getName().trim())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "onboarding template name already exists");
        }
        return null;
    }
}
