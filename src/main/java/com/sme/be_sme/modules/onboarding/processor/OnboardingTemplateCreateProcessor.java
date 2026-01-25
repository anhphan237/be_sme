package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTemplateCreateRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTemplateResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingTemplateCreateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingTemplateCreateRequest request = objectMapper.convertValue(payload, OnboardingTemplateCreateRequest.class);
        OnboardingTemplateResponse response = new OnboardingTemplateResponse();
        response.setTemplateId(UUID.randomUUID().toString());
        response.setName(request.getName());
        response.setStatus("DRAFT");
        return response;
    }
}
