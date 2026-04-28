package com.sme.be_sme.modules.platform.processor.template;

import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BizContext;
import org.springframework.util.StringUtils;

public final class PlatformTemplateBizHelper {

    private PlatformTemplateBizHelper() {
    }

    public static final String PLATFORM_COMPANY_ID = "00000000-0000-0000-0000-000000000001";
    public static final String LEVEL_PLATFORM = "PLATFORM";

    public static void assertPlatformAdmin(BizContext context, String actionName) {
        if (context == null
                || !StringUtils.hasText(context.getTenantId())
                || !PLATFORM_COMPANY_ID.equals(context.getTenantId().trim())) {
            throw AppException.of(
                    ErrorCodes.FORBIDDEN,
                    "platform template " + actionName + " requires platform admin");
        }
    }

    public static String requireTemplateId(String templateId) {
        if (!StringUtils.hasText(templateId)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "templateId is required");
        }
        return templateId.trim();
    }

    public static OnboardingTemplateEntity getPlatformTemplate(
            OnboardingTemplateMapper onboardingTemplateMapper,
            String templateId) {
        String id = requireTemplateId(templateId);

        OnboardingTemplateEntity entity = onboardingTemplateMapper.selectByPrimaryKey(id);
        if (entity == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "platform onboarding template not found");
        }

        if (!LEVEL_PLATFORM.equalsIgnoreCase(entity.getLevel())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "only PLATFORM templates can be managed here");
        }

        if (!PLATFORM_COMPANY_ID.equals(entity.getCompanyId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid platform template company");
        }

        return entity;
    }
}