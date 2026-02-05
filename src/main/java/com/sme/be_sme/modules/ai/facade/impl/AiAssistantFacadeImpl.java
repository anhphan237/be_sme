package com.sme.be_sme.modules.ai.facade.impl;

import com.sme.be_sme.modules.ai.api.request.AssistantAskRequest;
import com.sme.be_sme.modules.ai.api.response.AssistantAskResponse;
import com.sme.be_sme.modules.ai.facade.AiAssistantFacade;
import com.sme.be_sme.modules.ai.processor.AssistantAskProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiAssistantFacadeImpl extends BaseOperationFacade implements AiAssistantFacade {

    private final AssistantAskProcessor assistantAskProcessor;

    @Override
    public AssistantAskResponse ask(AssistantAskRequest request) {
        return call(assistantAskProcessor, request, AssistantAskResponse.class);
    }
}
