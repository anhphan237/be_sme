package com.sme.be_sme.modules.onboarding.facade.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.modules.onboarding.facade.OnboardingTaskFacade;
import com.sme.be_sme.shared.gateway.api.OperationStubResponse;
import org.springframework.stereotype.Component;

@Component
public class OnboardingTaskFacadeImpl implements OnboardingTaskFacade {

    @Override
    public OperationStubResponse generateTasksFromTemplate(JsonNode payload) {
        return OperationStubResponse.notImplemented("com.sme.onboarding.task.generate");
    }

    @Override
    public OperationStubResponse assignTask(JsonNode payload) {
        return OperationStubResponse.notImplemented("com.sme.onboarding.task.assign");
    }

    @Override
    public OperationStubResponse updateTaskStatus(JsonNode payload) {
        return OperationStubResponse.notImplemented("com.sme.onboarding.task.updateStatus");
    }
}
