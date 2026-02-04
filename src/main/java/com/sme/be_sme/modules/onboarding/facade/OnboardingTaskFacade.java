package com.sme.be_sme.modules.onboarding.facade;

import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskAssignRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskGenerateRequest;
import com.sme.be_sme.modules.onboarding.api.request.OnboardingTaskUpdateStatusRequest;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskGenerationResponse;
import com.sme.be_sme.modules.onboarding.api.response.OnboardingTaskResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface OnboardingTaskFacade extends OperationFacadeProvider {

    @OperationType("com.sme.onboarding.task.generate")
    OnboardingTaskGenerationResponse generateTasksFromTemplate(OnboardingTaskGenerateRequest request);

    @OperationType("com.sme.onboarding.task.assign")
    OnboardingTaskResponse assignTask(OnboardingTaskAssignRequest request);

    @OperationType("com.sme.onboarding.task.updateStatus")
    OnboardingTaskResponse updateTaskStatus(OnboardingTaskUpdateStatusRequest request);
}
