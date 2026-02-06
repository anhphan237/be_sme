package com.sme.be_sme.modules.automation.facade.impl;

import com.sme.be_sme.modules.automation.api.request.AutomationEmailSendRequest;
import com.sme.be_sme.modules.automation.api.response.AutomationEmailSendResponse;
import com.sme.be_sme.modules.automation.facade.AutomationFacade;
import com.sme.be_sme.modules.automation.processor.AutomationEmailSendProcessor;
import com.sme.be_sme.shared.gateway.core.BaseOperationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutomationFacadeImpl extends BaseOperationFacade implements AutomationFacade {

    private final AutomationEmailSendProcessor automationEmailSendProcessor;

    @Override
    public AutomationEmailSendResponse sendEmail(AutomationEmailSendRequest request) {
        return call(automationEmailSendProcessor, request, AutomationEmailSendResponse.class);
    }
}
