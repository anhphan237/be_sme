package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateAIGenerateRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateAIGenerateResponse;
import com.sme.be_sme.modules.onboarding.service.OnboardingTemplateGeneratorService;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class OnboardingTemplateAIGenerateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final OnboardingTemplateGeneratorService generatorService;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingTemplateAIGenerateRequest request = objectMapper.convertValue(payload, OnboardingTemplateAIGenerateRequest.class);

        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        OnboardingTemplateAIGenerateResponse response = generatorService.generate(
                context.getTenantId(),
                context.getOperatorId(),
                request.getIndustry(),
                request.getCompanySize(),
                request.getJobRole()
        );

        return response;
    }
}
