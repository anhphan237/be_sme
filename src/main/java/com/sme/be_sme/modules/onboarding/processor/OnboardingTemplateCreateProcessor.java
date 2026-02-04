package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateCreateRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateResponse;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.OnboardingTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.OnboardingTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingTemplateCreateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingTemplateMapper onboardingTemplateMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingTemplateCreateRequest request = objectMapper.convertValue(payload, OnboardingTemplateCreateRequest.class);
        if (!StringUtils.hasText(request.getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is required");
        }

        String templateId = UuidGenerator.generate();
        Date now = new Date();

        OnboardingTemplateEntity entity = new OnboardingTemplateEntity();
        entity.setOnboardingTemplateId(templateId);
        entity.setCompanyId(context.getTenantId());
        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription());
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "DRAFT");
        entity.setCreatedBy(StringUtils.hasText(request.getCreatedBy()) ? request.getCreatedBy() : "system");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        int inserted = onboardingTemplateMapper.insert(entity);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create onboarding template failed");
        }

        OnboardingTemplateResponse response = new OnboardingTemplateResponse();
        response.setTemplateId(templateId);
        response.setName(entity.getName());
        response.setStatus(entity.getStatus());
        return response;
    }
}
