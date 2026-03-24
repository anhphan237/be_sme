package com.sme.be_sme.modules.notification.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sme.be_sme.modules.automation.service.EmailSenderService;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.notification.infrastructure.mapper.NotificationMapper;
import com.sme.be_sme.modules.notification.infrastructure.persistence.entity.NotificationEntity;
import com.sme.be_sme.shared.util.UuidGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Map;

/**
 * Unified notification service: insert DB → send email (optional) → push WebSocket (if user online).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final EmailSenderService emailSenderService;
    private final NotificationPushService notificationPushService;
    private final UserMapperExt userMapperExt;
    private final ObjectMapper objectMapper;

    /**
     * Create notification, optionally send email, and push via WebSocket if user is online.
     *
     * @return the created notification ID, or null if userId is empty (skipped)
     */
    public String create(NotificationCreateParams params) {
        if (!StringUtils.hasText(params.getUserId())) {
            return null;
        }
        if (!StringUtils.hasText(params.getCompanyId())) {
            return null;
        }

        Date now = new Date();
        String notificationId = UuidGenerator.generate();

        NotificationEntity entity = new NotificationEntity();
        entity.setNotificationId(notificationId);
        entity.setCompanyId(params.getCompanyId());
        entity.setUserId(params.getUserId());
        entity.setType(params.getType());
        entity.setTitle(params.getTitle());
        entity.setContent(params.getContent());
        entity.setStatus("UNREAD");
        entity.setRefType(params.getRefType());
        entity.setRefId(params.getRefId());
        entity.setCreatedAt(now);
        notificationMapper.insert(entity);

        if (params.isSendEmail() && StringUtils.hasText(params.getEmailTemplate())) {
            sendEmail(params);
        }

        pushToWebSocket(params.getCompanyId(), params.getUserId(), notificationId, entity);

        return notificationId;
    }

    private void sendEmail(NotificationCreateParams params) {
        String toEmail = params.getToEmail();
        if (!StringUtils.hasText(toEmail)) {
            UserEntity user = userMapperExt.selectByCompanyIdAndUserId(params.getCompanyId(), params.getUserId());
            if (user != null && StringUtils.hasText(user.getEmail())) {
                toEmail = user.getEmail();
            }
        }
        if (!StringUtils.hasText(toEmail)) {
            log.debug("NotificationService: skip email, no recipient for user {}", params.getUserId());
            return;
        }
        try {
            Map<String, String> placeholders = params.getEmailPlaceholders() != null ? params.getEmailPlaceholders() : Map.of();
            emailSenderService.sendWithTemplate(params.getCompanyId(), params.getEmailTemplate(), toEmail,
                    placeholders, params.getUserId(), params.getOnboardingId());
        } catch (Exception e) {
            log.warn("NotificationService: email failed for {}: {}", params.getUserId(), e.getMessage());
        }
    }

    private void pushToWebSocket(String companyId, String userId, String notificationId, NotificationEntity entity) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("notificationId", notificationId);
            payload.put("type", entity.getType());
            payload.put("title", entity.getTitle());
            payload.put("content", entity.getContent());
            payload.put("refType", entity.getRefType());
            payload.put("refId", entity.getRefId());
            payload.put("createdAt", entity.getCreatedAt() != null ? entity.getCreatedAt().getTime() : 0);
            notificationPushService.pushToUser(companyId, userId, payload);
        } catch (Exception e) {
            log.debug("NotificationService: WebSocket push failed (user may be offline): {}", e.getMessage());
        }
    }
}
