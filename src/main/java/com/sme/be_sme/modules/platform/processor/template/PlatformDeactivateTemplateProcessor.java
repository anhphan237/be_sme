package com.sme.be_sme.modules.platform.processor.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.modules.platform.api.request.DeactivatePlatformTemplateRequest;
import com.sme.be_sme.modules.platform.api.response.CreatePlatformTemplateResponse;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class PlatformDeactivateTemplateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingTemplateMapper onboardingTemplateMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        PlatformTemplateBizHelper.assertPlatformAdmin(context, "deactivate");

        DeactivatePlatformTemplateRequest request =
                objectMapper.convertValue(payload, DeactivatePlatformTemplateRequest.class);

        String templateId = PlatformTemplateBizHelper.requireTemplateId(
                request == null ? null : request.getTemplateId());

        OnboardingTemplateEntity entity =
                PlatformTemplateBizHelper.getPlatformTemplate(onboardingTemplateMapper, templateId);

        if ("ARCHIVED".equalsIgnoreCase(entity.getStatus())) {
            return toResponse(entity);
        }

        entity.setStatus("ARCHIVED");
        entity.setUpdatedAt(new Date());

        if (onboardingTemplateMapper.updateByPrimaryKey(entity) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "deactivate platform template failed");
        }

        return toResponse(entity);
    }

    private static CreatePlatformTemplateResponse toResponse(OnboardingTemplateEntity entity) {
        CreatePlatformTemplateResponse response = new CreatePlatformTemplateResponse();
        response.setTemplateId(entity.getOnboardingTemplateId());
        response.setName(entity.getName());
        response.setStatus(entity.getStatus());
        response.setTemplateKind(entity.getTemplateKind());
        response.setDepartmentTypeCode(entity.getDepartmentTypeCode());
        response.setLevel(entity.getLevel());
        return response;
    }
}