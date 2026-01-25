package com.sme.be_sme.modules.onboarding.facade;

import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceActivateRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceCreateRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface OnboardingInstanceFacade extends OperationFacadeProvider {

    @OperationType("com.sme.onboarding.instance.create")
    OnboardingInstanceResponse createOnboardingInstance(OnboardingInstanceCreateRequest request);

    @OperationType("com.sme.onboarding.instance.activate")
    OnboardingInstanceResponse activateOnboardingInstance(OnboardingInstanceActivateRequest request);
}
