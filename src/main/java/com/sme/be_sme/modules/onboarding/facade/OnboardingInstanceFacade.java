package com.sme.be_sme.modules.onboarding.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.api.OperationStubResponse;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface OnboardingInstanceFacade extends OperationFacadeProvider {

    @OperationType("com.sme.onboarding.instance.create")
    OperationStubResponse createOnboardingInstance(JsonNode payload);

    @OperationType("com.sme.onboarding.instance.activate")
    OperationStubResponse activateOnboardingInstance(JsonNode payload);
}
