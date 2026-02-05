package com.sme.be_sme.modules.onboarding.processor.template.core;

import com.sme.be_sme.modules.onboarding.context.OnboardingTemplateCreateContext;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingTemplateCreateInsertTemplateCoreProcessor extends BaseCoreProcessor<OnboardingTemplateCreateContext> {

    private final OnboardingTemplateMapper onboardingTemplateMapper;

    @Override
    protected Object process(OnboardingTemplateCreateContext ctx) {
        OnboardingTemplateEntity entity = new OnboardingTemplateEntity();
        entity.setOnboardingTemplateId(ctx.getTemplateId());
        entity.setCompanyId(ctx.getCompanyId());
        entity.setName(ctx.getRequest().getName().trim());
        entity.setDescription(ctx.getRequest().getDescription());
        entity.setStatus(StringUtils.hasText(ctx.getRequest().getStatus()) ? ctx.getRequest().getStatus() : "DRAFT");
        entity.setCreatedBy(StringUtils.hasText(ctx.getRequest().getCreatedBy()) ? ctx.getRequest().getCreatedBy() : "system");
        entity.setCreatedAt(ctx.getNow());
        entity.setUpdatedAt(ctx.getNow());

        if (onboardingTemplateMapper.insert(entity) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create onboarding template failed");
        }
        ctx.setTemplateEntity(entity);
        return null;
    }
}
