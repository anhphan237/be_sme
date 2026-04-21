package com.sme.be_sme.modules.automation.job;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.EventInstanceMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.mapper.EventTemplateMapper;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventInstanceEntity;
import com.sme.be_sme.modules.onboarding.infrastructure.persistence.entity.EventTemplateEntity;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventReminderNotificationJob {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private final EventInstanceMapper eventInstanceMapper;
    private final EventTemplateMapper eventTemplateMapper;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "${app.automation.event-reminder.cron:0 */5 * * * ?}")
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        Date windowStart = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        Date windowEnd = Date.from(now.plusMinutes(15).atZone(ZoneId.systemDefault()).toInstant());
        List<EventInstanceEntity> events = eventInstanceMapper.selectReadyToNotify(windowStart, windowEnd, 200);
        if (CollectionUtils.isEmpty(events)) {
            return;
        }

        for (EventInstanceEntity event : events) {
            try {
                notifyOneEvent(event);
            } catch (Exception e) {
                log.warn("EventReminderNotificationJob failed, eventInstanceId={}: {}", event.getEventInstanceId(), e.getMessage());
            }
        }
    }

    private void notifyOneEvent(EventInstanceEntity event) {
        int locked = eventInstanceMapper.markNotifiedIfPending(event.getEventInstanceId(), new Date());
        if (locked != 1) {
            return;
        }

        EventTemplateEntity template = eventTemplateMapper.selectByCompanyIdAndTemplateId(
                event.getCompanyId(),
                event.getEventTemplateId());
        if (template == null) {
            return;
        }

        List<String> participantUserIds = parseParticipantUserIds(event.getParticipantUserIds());
        if (CollectionUtils.isEmpty(participantUserIds)) {
            return;
        }

        String eventName = StringUtils.hasText(template.getName()) ? template.getName().trim() : "Event";
        LocalDateTime eventAt = event.getEventAt() == null
                ? null
                : event.getEventAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        String eventAtText = eventAt == null ? "" : eventAt.format(DATE_TIME_FMT);
        String title = "Event starts in 15 minutes: " + eventName;
        String content = "Event \"" + eventName + "\" starts at " + eventAtText + ".";

        for (String userId : participantUserIds) {
            if (!StringUtils.hasText(userId)) {
                continue;
            }
            NotificationCreateParams params = NotificationCreateParams.builder()
                    .companyId(event.getCompanyId())
                    .userId(userId.trim())
                    .type("EVENT_REMINDER")
                    .title(title)
                    .content(content)
                    .refType("EVENT")
                    .refId(event.getEventInstanceId())
                    .build();
            notificationService.create(params);
        }
    }

    private List<String> parseParticipantUserIds(String participantUserIdsRaw) {
        if (!StringUtils.hasText(participantUserIdsRaw)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(participantUserIdsRaw, STRING_LIST_TYPE);
        } catch (Exception e) {
            log.warn("cannot parse participant_user_ids: {}", e.getMessage());
            return List.of();
        }
    }
}
