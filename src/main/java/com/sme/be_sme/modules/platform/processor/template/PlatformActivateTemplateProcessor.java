package com.sme.be_sme.modules.platform.processor.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.modules.platform.api.request.ActivatePlatformTemplateRequest;
import com.sme.be_sme.modules.platform.api.response.CreatePlatformTemplateResponse;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PlatformActivateTemplateProcessor extends BaseBizProcessor<BizContext> {

    /** Seed company for platform admin (V37). Only this tenant may activate platform templates. */
    private static final String PLATFORM_COMPANY_ID = "00000000-0000-0000-0000-000000000001";

    private static final String LEVEL_PLATFORM = "PLATFORM";

    private final ObjectMapper objectMapper;
    private final OnboardingTemplateMapper onboardingTemplateMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    protected Object doProcess(BizContext context, JsonNode payload) {
        ActivatePlatformTemplateRequest request =
                objectMapper.convertValue(payload, ActivatePlatformTemplateRequest.class);

        if (!StringUtils.hasText(context.getTenantId())
                || !PLATFORM_COMPANY_ID.equals(context.getTenantId().trim())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "platform template activation requires platform admin");
        }

        if (request == null || !StringUtils.hasText(request.getTemplateId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "templateId is required");
        }

        String templateId = request.getTemplateId().trim();
        OnboardingTemplateEntity entity = onboardingTemplateMapper.selectByPrimaryKey(templateId);
        if (entity == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "onboarding template not found");
        }

        if (!LEVEL_PLATFORM.equalsIgnoreCase(entity.getLevel())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "only PLATFORM templates can be activated here");
        }

        if (!PLATFORM_COMPANY_ID.equals(entity.getCompanyId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid platform template company");
        }

        String current = entity.getStatus() != null ? entity.getStatus().trim() : "";
        if ("ACTIVE".equalsIgnoreCase(current)) {
            return toResponse(entity);
        }
        if (!"DRAFT".equalsIgnoreCase(current)) {
            throw AppException.of(
                    ErrorCodes.BAD_REQUEST, "only DRAFT templates can be activated (current status: " + current + ")");
        }

        entity.setStatus("ACTIVE");
        entity.setUpdatedAt(new Date());
        if (onboardingTemplateMapper.updateByPrimaryKey(entity) != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "activate platform template failed");
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
