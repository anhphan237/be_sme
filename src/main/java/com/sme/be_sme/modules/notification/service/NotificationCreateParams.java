package com.sme.be_sme.modules.notification.service;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Parameters for creating a notification. NotificationService will:
 * 1) Insert into DB
 * 2) Send email if sendEmail=true and template/placeholders provided
 * 3) Push via WebSocket if user is online
 */
@Getter
@Builder
public class NotificationCreateParams {

    private final String companyId;
    private final String userId;
    private final String type;
    private final String title;
    private final String content;
    private final String refType;
    private final String refId;

    /**
     * If true and emailTemplate + toEmail are set, send email via template.
     */
    @Builder.Default
    private final boolean sendEmail = false;

    /**
     * Email template code (e.g. TASK_REMINDER, PAYMENT_REMINDER). Used when sendEmail=true.
     */
    private final String emailTemplate;

    /**
     * Placeholders for email template. Used when sendEmail=true.
     */
    private final Map<String, String> emailPlaceholders;

    /**
     * Recipient email. If null, resolved from userId + companyId when possible.
     */
    private final String toEmail;

    private final String onboardingId;
}
