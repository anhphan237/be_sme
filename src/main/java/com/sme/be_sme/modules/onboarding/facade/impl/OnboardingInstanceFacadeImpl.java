package com.sme.be_sme.modules.onboarding.facade.impl;

import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceActivateRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceCancelRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceCompleteRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceCreateRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceGetRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceListRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceDetailResponse;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceListResponse;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceResponse;
import com.sme.be_sme.modules.onboarding.facade.OnboardingInstanceFacade;
import com.sme.be_sme.modules.onboarding.processor.OnboardingInstanceActivateProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingInstanceCancelProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingInstanceCompleteProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingInstanceCreateProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingInstanceGetProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingInstanceListProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingInstanceFacadeImpl extends BaseOperationFacade implements OnboardingInstanceFacade {

    private final OnboardingInstanceCreateProcessor onboardingInstanceCreateProcessor;
    private final OnboardingInstanceActivateProcessor onboardingInstanceActivateProcessor;
    private final OnboardingInstanceListProcessor onboardingInstanceListProcessor;
    private final OnboardingInstanceGetProcessor onboardingInstanceGetProcessor;
    private final OnboardingInstanceCancelProcessor onboardingInstanceCancelProcessor;
    private final OnboardingInstanceCompleteProcessor onboardingInstanceCompleteProcessor;

    @Override
    public OnboardingInstanceResponse createOnboardingInstance(OnboardingInstanceCreateRequest request) {
        return call(onboardingInstanceCreateProcessor, request, OnboardingInstanceResponse.class);
    }

    @Override
    public OnboardingInstanceResponse activateOnboardingInstance(OnboardingInstanceActivateRequest request) {
        return call(onboardingInstanceActivateProcessor, request, OnboardingInstanceResponse.class);
    }

    @Override
    public OnboardingInstanceListResponse listOnboardingInstances(OnboardingInstanceListRequest request) {
        return call(onboardingInstanceListProcessor, request, OnboardingInstanceListResponse.class);
    }

    @Override
    public OnboardingInstanceDetailResponse getOnboardingInstance(OnboardingInstanceGetRequest request) {
        return call(onboardingInstanceGetProcessor, request, OnboardingInstanceDetailResponse.class);
    }

    @Override
    public OnboardingInstanceResponse cancelOnboardingInstance(OnboardingInstanceCancelRequest request) {
        return call(onboardingInstanceCancelProcessor, request, OnboardingInstanceResponse.class);
    }

    @Override
    public OnboardingInstanceResponse completeOnboardingInstance(OnboardingInstanceCompleteRequest request) {
        return call(onboardingInstanceCompleteProcessor, request, OnboardingInstanceResponse.class);
    }
}
