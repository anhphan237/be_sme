package com.sme.be_sme.modules.onboarding.facade.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.modules.onboarding.facade.OnboardingTemplateFacade;
import com.sme.be_sme.shared.gateway.api.OperationStubResponse;
import org.springframework.stereotype.Component;

@Component
public class OnboardingTemplateFacadeImpl implements OnboardingTemplateFacade {

    @Override
    public OperationStubResponse createOnboardingTemplate(JsonNode payload) {
        return OperationStubResponse.notImplemented("com.sme.onboarding.template.create");
    }

    @Override
    public OperationStubResponse updateOnboardingTemplate(JsonNode payload) {
        return OperationStubResponse.notImplemented("com.sme.onboarding.template.update");
    }
}
