package com.sme.be_sme.modules.company.processor.registration;

import com.sme.be_sme.modules.company.context.CompanyRegisterContext;
import com.sme.be_sme.modules.onboarding.service.OnboardingTemplateGeneratorService;
import com.sme.be_sme.shared.gateway.core.BaseCoreProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyRegisterGenerateOnboardingCoreProcessor extends BaseCoreProcessor<CompanyRegisterContext> {

    private final OnboardingTemplateGeneratorService generatorService;

    @Override
    protected Object process(CompanyRegisterContext ctx) {
        String industry = ctx.getRequest().getCompany().getIndustry();
        if (!StringUtils.hasText(industry)) {
            return null;
        }

        String companyId = ctx.getCompanyId();
        String adminUserId = ctx.getAdminUser() != null ? ctx.getAdminUser().getUserId() : "system";
        String companySize = ctx.getRequest().getCompany().getCompanySize();

        try {
            generatorService.generate(companyId, adminUserId, industry, companySize, null);
            log.info("AI onboarding template generated for company {}", companyId);
        } catch (Exception e) {
            log.warn("Failed to auto-generate onboarding template for company {}: {}", companyId, e.getMessage());
        }
        return null;
    }
}
