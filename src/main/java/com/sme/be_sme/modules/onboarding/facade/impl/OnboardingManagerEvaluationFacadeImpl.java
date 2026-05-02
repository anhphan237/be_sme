package com.sme.be_sme.modules.onboarding.facade.impl;

import com.sme.be_sme.modules.onboarding.api.request.OnboardingManagerEvaluationSendRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingManagerEvaluationStatusRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingManagerEvaluationSendResponse;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingManagerEvaluationStatusResponse;
import com.sme.be_sme.modules.onboarding.facade.OnboardingManagerEvaluationFacade;
import com.sme.be_sme.modules.onboarding.processor.OnboardingManagerEvaluationSendProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingManagerEvaluationStatusProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingManagerEvaluationFacadeImpl extends BaseOperationFacade implements OnboardingManagerEvaluationFacade {

    private final OnboardingManagerEvaluationStatusProcessor onboardingManagerEvaluationStatusProcessor;
    private final OnboardingManagerEvaluationSendProcessor onboardingManagerEvaluationSendProcessor;

    @Override
    public OnboardingManagerEvaluationStatusResponse status(OnboardingManagerEvaluationStatusRequest request) {
        return call(
                onboardingManagerEvaluationStatusProcessor,
                request,
                OnboardingManagerEvaluationStatusResponse.class
        );
    }

    @Override
    public OnboardingManagerEvaluationSendResponse send(OnboardingManagerEvaluationSendRequest request) {
        return call(
                onboardingManagerEvaluationSendProcessor,
                request,
                OnboardingManagerEvaluationSendResponse.class
        );
    }
}