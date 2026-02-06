package com.sme.be_sme.modules.automation.facade;

import com.sme.be_sme.modules.automation.api.request.AutomationEmailSendRequest;
import com.sme.be_sme.modules.automation.api.response.AutomationEmailSendResponse;
import com.sme.be_sme.shared.gateway.annotation.OperationType;
import com.sme.be_sme.shared.gateway.core.OperationFacadeProvider;

public interface AutomationFacade extends OperationFacadeProvider {

    @OperationType("com.sme.automation.email.send")
    AutomationEmailSendResponse sendEmail(AutomationEmailSendRequest request);
}
