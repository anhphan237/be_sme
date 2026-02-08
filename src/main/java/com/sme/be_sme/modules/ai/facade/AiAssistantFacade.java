package com.sme.be_sme.modules.ai.facade;

import com.sme.be_sme.modules.ai.api.request.AssistantAskRequest;
import com.sme.be_sme.modules.ai.api.response.AssistantAskResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface AiAssistantFacade extends OperationFacadeProvider {

    @OperationType("com.sme.ai.assistant.ask")
    AssistantAskResponse ask(AssistantAskRequest request);
}
