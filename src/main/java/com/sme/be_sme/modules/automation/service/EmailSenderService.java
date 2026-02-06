package com.sme.be_sme.modules.automation.service;

import com.sme.be_sme.modules.automation.infrastructure.mapper.EmailLogMapper;
import com.sme.be_sme.modules.automation.infrastructure.mapper.EmailTemplateMapperExt;
import com.sme.be_sme.modules.automation.infrastructure.persistence.entity.EmailLogEntity;
import com.sme.be_sme.modules.automation.infrastructure.persistence.entity.EmailTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sends email via SMTP (or configured provider). Resolves template by code, replaces placeholders, logs to email_logs.
 * If JavaMailSender is not configured (optional), send is no-op and logged as FAILED.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSenderService {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private static final String PROVIDER_SMTP = "SMTP";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";

    private final EmailTemplateMapperExt emailTemplateMapperExt;
    private final EmailLogMapper emailLogMapper;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    /**
     * Send email with subject and body (no template). Optionally set userId/onboardingId on log.
     */
    public void sendEmail(String companyId, String toEmail, String subject, String body, String templateCode,
                          String userId, String onboardingId) {
        if (!StringUtils.hasText(toEmail)) {
            log.warn("EmailSenderService: skip send, toEmail empty");
            return;
        }
        Date now = new Date();
        EmailLogEntity logEntity = new EmailLogEntity();
        logEntity.setEmailLogId(UuidGenerator.generate());
        logEntity.setCompanyId(companyId);
        logEntity.setToEmail(toEmail.trim());
        logEntity.setSubject(subject != null ? subject : "");
        logEntity.setProvider(PROVIDER_SMTP);
        logEntity.setTemplateCode(templateCode);
        logEntity.setQueuedAt(now);
        if (StringUtils.hasText(userId)) logEntity.setUserId(userId);
        if (StringUtils.hasText(onboardingId)) logEntity.setOnboardingId(onboardingId);

        try {
            if (mailSender == null) {
                log.warn("EmailSenderService: JavaMailSender not configured, skip send to {}", toEmail);
                logEntity.setStatus(STATUS_FAILED);
                logEntity.setErrorMessage("Mail sender not configured");
            } else {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(toEmail.trim());
                msg.setSubject(subject != null ? subject : "");
                msg.setText(body != null ? body : "");
                mailSender.send(msg);
                logEntity.setStatus(STATUS_SENT);
                logEntity.setSentAt(new Date());
            }
        } catch (Exception e) {
            log.warn("EmailSenderService: send failed to {}: {}", toEmail, e.getMessage());
            logEntity.setStatus(STATUS_FAILED);
            logEntity.setErrorMessage(e.getMessage());
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "Failed to send email: " + e.getMessage());
        } finally {
            emailLogMapper.insert(logEntity);
        }
    }

    /**
     * Resolve template by code (company + name), replace placeholders, then send. Logs to email_logs with templateCode.
     */
    public void sendWithTemplate(String companyId, String templateCode, String toEmail,
                                Map<String, String> placeholders,
                                String userId, String onboardingId) {
        if (!StringUtils.hasText(toEmail)) {
            log.warn("EmailSenderService: skip sendWithTemplate, toEmail empty");
            return;
        }
        EmailTemplateEntity template = emailTemplateMapperExt.selectByCompanyIdAndName(companyId, templateCode);
        if (template == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "Email template not found: " + templateCode);
        }
        String subject = replacePlaceholders(template.getSubjectTemplate(), placeholders);
        String body = replacePlaceholders(template.getBodyTemplate(), placeholders);
        sendEmail(companyId, toEmail, subject, body, templateCode, userId, onboardingId);
    }

    /**
     * Replace {{key}} in text with values from map (key = placeholder name).
     */
    public static String replacePlaceholders(String text, Map<String, String> placeholders) {
        if (text == null) return "";
        if (placeholders == null || placeholders.isEmpty()) return text;
        StringBuffer sb = new StringBuffer();
        Matcher m = PLACEHOLDER.matcher(text);
        while (m.find()) {
            String key = m.group(1).trim();
            String value = placeholders.getOrDefault(key, "");
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
