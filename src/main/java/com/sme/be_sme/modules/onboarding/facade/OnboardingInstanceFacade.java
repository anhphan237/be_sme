package com.sme.be_sme.modules.onboarding.facade;

import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceActivateRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceCancelRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceCreateRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceGetRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingInstanceListRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceDetailResponse;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceListResponse;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingInstanceResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface OnboardingInstanceFacade extends OperationFacadeProvider {

    @OperationType("com.sme.onboarding.instance.create")
    OnboardingInstanceResponse createOnboardingInstance(OnboardingInstanceCreateRequest request);

    @OperationType("com.sme.onboarding.instance.activate")
    OnboardingInstanceResponse activateOnboardingInstance(OnboardingInstanceActivateRequest request);

    @OperationType("com.sme.onboarding.instance.list")
    OnboardingInstanceListResponse listOnboardingInstances(OnboardingInstanceListRequest request);

    @OperationType("com.sme.onboarding.instance.get")
    OnboardingInstanceDetailResponse getOnboardingInstance(OnboardingInstanceGetRequest request);

    @OperationType("com.sme.onboarding.instance.cancel")
    OnboardingInstanceResponse cancelOnboardingInstance(OnboardingInstanceCancelRequest request);
}
