package com.sme.be_sme.modules.platform.processor.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.modules.platform.api.request.DeletePlatformTemplateRequest;
import com.sme.be_sme.modules.platform.api.response.DeletePlatformTemplateResponse;
import com.sme.be_sme.modules.platform.infrastructure.mapper.PlatformTemplateMapperExt;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PlatformDeleteTemplateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingTemplateMapper onboardingTemplateMapper;
    private final PlatformTemplateMapperExt platformTemplateMapperExt;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformTemplateBizHelper.assertPlatformAdmin(context, "delete");

        DeletePlatformTemplateRequest request =
                objectMapper.convertValue(payload, DeletePlatformTemplateRequest.class);

        String templateId = PlatformTemplateBizHelper.requireTemplateId(
                request == null ? null : request.getTemplateId());

        OnboardingTemplateEntity entity =
                PlatformTemplateBizHelper.getPlatformTemplate(onboardingTemplateMapper, templateId);

        int usageCount = platformTemplateMapperExt.countTemplateUsage(templateId);
        if (usageCount > 0) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST,
                    "template already used; archive it instead of deleting");
        }

        platformTemplateMapperExt.deleteRequiredDocumentsByTemplateId(
                PlatformTemplateBizHelper.PLATFORM_COMPANY_ID,
                templateId
        );
        platformTemplateMapperExt.deleteTasksByTemplateId(
                PlatformTemplateBizHelper.PLATFORM_COMPANY_ID,
                templateId
        );
        platformTemplateMapperExt.deleteChecklistsByTemplateId(
                PlatformTemplateBizHelper.PLATFORM_COMPANY_ID,
                templateId
        );

        int deleted = platformTemplateMapperExt.deleteTemplateById(
                PlatformTemplateBizHelper.PLATFORM_COMPANY_ID,
                templateId
        );

        if (deleted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "delete platform template failed");
        }

        DeletePlatformTemplateResponse response = new DeletePlatformTemplateResponse();
        response.setTemplateId(entity.getOnboardingTemplateId());
        response.setDeleted(true);
        return response;
    }
}