package com.sme.be_sme.modules.automation.api.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AutomationEmailSendResponse {
    private boolean success;
    private String message;
}
