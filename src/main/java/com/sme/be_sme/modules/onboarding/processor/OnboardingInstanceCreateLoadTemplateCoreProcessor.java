package com.sme.be_sme.modules.onboarding.processor;

import com.sme.be_sme.modules.onboarding.context.OnboardingInstanceCreateContext;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapperExt;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceCreateLoadTemplateCoreProcessor
        extends BaseCoreProcessor<OnboardingInstanceCreateContext> {

    private final OnboardingTemplateMapperExt templateMapperExt;

    @Override
    protected Object process(OnboardingInstanceCreateContext ctx) {
        String companyId = ctx.getBiz().getTenantId();
        String templateId = ctx.getRequest().getTemplateId();

        OnboardingTemplateEntity tpl =
                templateMapperExt.selectTemplateByIdAndCompany(templateId, companyId);
        if (tpl == null) {
            throw AppException.of("TEMPLATE_NOT_FOUND", "template not found");
        }

        ctx.setTemplate(tpl);
        ctx.setChecklistRows(templateMapperExt.selectChecklistRows(companyId, templateId));
        ctx.setBaselineTaskRows(templateMapperExt.selectBaselineTaskRows(companyId, templateId));
        return null;
    }
}

