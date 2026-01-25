package com.sme.be_sme.modules.onboarding.facade;

import com.fasterxml.jackson.databind.JsonNode;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.api.OperationStubResponse;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface OnboardingTaskFacade extends OperationFacadeProvider {

    @OperationType("com.sme.onboarding.task.generate")
    OperationStubResponse generateTasksFromTemplate(JsonNode payload);

    @OperationType("com.sme.onboarding.task.assign")
    OperationStubResponse assignTask(JsonNode payload);

    @OperationType("com.sme.onboarding.task.updateStatus")
    OperationStubResponse updateTaskStatus(JsonNode payload);
}
