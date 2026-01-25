package com.sme.be_sme.modules.onboarding.facade;

import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskAssignRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskGenerateRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskUpdateStatusRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskGenerationResponse;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskResponse;
import com.sme.be_sme.modules.onboarding.processor.OnboardingTaskAssignProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingTaskGenerateProcessor;
import com.sme.be_sme.modules.onboarding.processor.OnboardingTaskUpdateStatusProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnboardingTaskFacadeImpl extends BaseOperationFacade implements OnboardingTaskFacade {

    private final OnboardingTaskGenerateProcessor onboardingTaskGenerateProcessor;
    private final OnboardingTaskAssignProcessor onboardingTaskAssignProcessor;
    private final OnboardingTaskUpdateStatusProcessor onboardingTaskUpdateStatusProcessor;

    @Override
    public OnboardingTaskGenerationResponse generateTasksFromTemplate(OnboardingTaskGenerateRequest request) {
        return call(onboardingTaskGenerateProcessor, request, OnboardingTaskGenerationResponse.class);
    }

    @Override
    public OnboardingTaskResponse assignTask(OnboardingTaskAssignRequest request) {
        return call(onboardingTaskAssignProcessor, request, OnboardingTaskResponse.class);
    }

    @Override
    public OnboardingTaskResponse updateTaskStatus(OnboardingTaskUpdateStatusRequest request) {
        return call(onboardingTaskUpdateStatusProcessor, request, OnboardingTaskResponse.class);
    }
}
