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
    private static final String LEVEL_PLATFORM = "PLATFORM";
    private static final String STATUS_ACTIVE = "ACTIVE";

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
        if (LEVEL_PLATFORM.equalsIgnoreCase(tpl.getLevel())) {
            throw AppException.of(
                    "INVALID_TEMPLATE_LEVEL",
                    "platform template is view-only, please clone to tenant template before onboarding");
        }
        if (!STATUS_ACTIVE.equalsIgnoreCase(tpl.getStatus())) {
            throw AppException.of(
                    "INVALID_TEMPLATE_STATUS",
                    "template must be ACTIVE before onboarding");
        }

        ctx.setTemplate(tpl);
        ctx.setChecklistRows(templateMapperExt.selectChecklistRows(companyId, templateId));
        ctx.setBaselineTaskRows(templateMapperExt.selectBaselineTaskRows(companyId, templateId));
        return null;
    }
}

