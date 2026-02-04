package com.sme.be_sme.modules.onboarding.processor.template.core;

import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateGetContext;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingTemplateGetLoadCoreProcessor extends BaseCoreProcessor<OnboardingTemplateGetContext> {

    private final OnboardingTemplateMapperExt onboardingTemplateMapperExt;

    @Override
    protected Object process(OnboardingTemplateGetContext ctx) {
        String companyId = ctx.getBiz().getTenantId();
        String templateId = ctx.getRequest().getTemplateId();

        OnboardingTemplateEntity template = onboardingTemplateMapperExt.selectTemplateByIdAndCompany(templateId, companyId);
        if (template == null) {
            throw AppException.of("TEMPLATE_NOT_FOUND", "template not found");
        }

        ctx.setTemplate(template);
        ctx.setChecklistRows(onboardingTemplateMapperExt.selectChecklistRows(companyId, templateId));
        ctx.setBaselineTaskRows(onboardingTemplateMapperExt.selectBaselineTaskRows(companyId, templateId));
        return null;
    }
}
