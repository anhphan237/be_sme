package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateUpdateRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingTemplateUpdateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingTemplateUpdateRequest request = objectMapper.convertValue(payload, OnboardingTemplateUpdateRequest.class);
        OnboardingTemplateResponse response = new OnboardingTemplateResponse();
        response.setTemplateId(request.getTemplateId());
        response.setName(request.getName());
        response.setStatus(request.getStatus());
        return response;
    }
}
