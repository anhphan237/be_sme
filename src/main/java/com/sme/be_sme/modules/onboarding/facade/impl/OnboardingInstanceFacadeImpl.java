package com.sme.be_sme.modules.onboarding.facade.impl;

import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceActivateRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceCreateRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceResponse;
import com.sme.be_sme.modules.onboarding.facade.OnboardingInstanceFacade;
import com.sme.be_sme.modules.onboarding.processor.OnboardingInstanceActivateProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingInstanceCreateProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceFacadeImpl extends BaseOperationFacade implements OnboardingInstanceFacade {

    private final OnboardingInstanceCreateProcessor onboardingInstanceCreateProcessor;
    private final OnboardingInstanceActivateProcessor onboardingInstanceActivateProcessor;

    @Override
    public OnboardingInstanceResponse createOnboardingInstance(OnboardingInstanceCreateRequest request) {
        return call(onboardingInstanceCreateProcessor, request, OnboardingInstanceResponse.class);
    }

    @Override
    public OnboardingInstanceResponse activateOnboardingInstance(OnboardingInstanceActivateRequest request) {
        return call(onboardingInstanceActivateProcessor, request, OnboardingInstanceResponse.class);
    }
}
