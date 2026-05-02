package com.sme.be_sme.modules.onboarding.facade;

import com.sme.be_sme.modules.onboarding.api.request.OnboardingManagerEvaluationSendRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingManagerEvaluationStatusRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingManagerEvaluationSendResponse;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingManagerEvaluationStatusResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;

public interface OnboardingManagerEvaluationFacade {
    @OperationType("com.sme.onboarding.managerEvaluation.status")
    OnboardingManagerEvaluationStatusResponse status(OnboardingManagerEvaluationStatusRequest request);

    @OperationType("com.sme.onboarding.managerEvaluation.send")
    OnboardingManagerEvaluationSendResponse send(OnboardingManagerEvaluationSendRequest request);

}
