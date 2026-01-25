package com.sme.be_sme.modules.onboarding.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskAssignRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskResponse;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingTaskAssignProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        OnboardingTaskAssignRequest request = objectMapper.convertValue(payload, OnboardingTaskAssignRequest.class);
        OnboardingTaskResponse response = new OnboardingTaskResponse();
        response.setTaskId(request.getTaskId());
        response.setAssigneeUserId(request.getAssigneeUserId());
        response.setStatus("ASSIGNED");
        return response;
    }
}
