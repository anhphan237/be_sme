package com.sme.be_sme.modules.automation.api.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class AutomationEmailSendRequest {
    /** Template code (e.g. WELCOME_NEW_EMPLOYEE, TASK_REMINDER) */
    private String templateCode;
    /** Recipient email */
    private String toEmail;
    /** Optional placeholders (e.g. employeeName, companyName, dueDate) for testing */
    private Map<String, String> placeholders;
}
