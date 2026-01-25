package com.sme.be_sme.modules.onboarding.facade.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.modules.onboarding.facade.OnboardingInstanceFacade;
import com.sme.be_sme.shared.gateway.api.OperationStubResponse;
import org.springframework.stereotype.Component;

@Component
public class OnboardingInstanceFacadeImpl implements OnboardingInstanceFacade {

    @Override
    public OperationStubResponse createOnboardingInstance(JsonNode payload) {
        return OperationStubResponse.notImplemented("com.sme.onboarding.instance.create");
    }

    @Override
    public OperationStubResponse activateOnboardingInstance(JsonNode payload) {
        return OperationStubResponse.notImplemented("com.sme.onboarding.instance.activate");
    }
}
