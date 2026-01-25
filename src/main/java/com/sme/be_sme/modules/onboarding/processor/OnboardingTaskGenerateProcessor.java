package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskGenerateRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskGenerationResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingTaskGenerateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingTaskGenerateRequest request = objectMapper.convertValue(payload, OnboardingTaskGenerateRequest.class);
        OnboardingTaskGenerationResponse response = new OnboardingTaskGenerationResponse();
        response.setInstanceId(request.getInstanceId());
        response.setTotalTasks(0);
        return response;
    }
}
