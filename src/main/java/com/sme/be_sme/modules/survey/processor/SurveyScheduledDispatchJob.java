package com.sme.be_sme.modules.survey.processor;

import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyInstanceEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyTemplateEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SurveyScheduledDispatchJob {

    private static final int DEFAULT_DUE_DAYS = 7;
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SurveyInstanceMapperExt surveyInstanceMapperExt;
    private final SurveyInstanceMapper surveyInstanceMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;
    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 60000)
    public void dispatchScheduledSurveys() {
        Date now = new Date();

        List<SurveyInstanceEntity> instances =
                surveyInstanceMapperExt.selectScheduledReadyToDispatch(now, 100);

        if (instances == null || instances.isEmpty()) {
            return;
        }

        for (SurveyInstanceEntity item : instances) {
            try {
                SurveyTemplateEntity template =
                        surveyTemplateMapper.selectByPrimaryKey(item.getSurveyTemplateId());

                if (template == null || !"ACTIVE".equalsIgnoreCase(template.getStatus())) {
                    continue;
                }

                if (item.getClosedAt() != null && !item.getClosedAt().after(now)) {
                    item.setStatus("EXPIRED");
                    trySetUpdatedAt(item, now);
                    surveyInstanceMapper.updateByPrimaryKey(item);
                    continue;
                }

                int updated = surveyInstanceMapperExt.markAsSentIfScheduled(
                        item.getSurveyInstanceId(),
                        now
                );

                if (updated != 1) {
                    continue;
                }

                SurveyInstanceEntity fresh =
                        surveyInstanceMapper.selectByPrimaryKey(item.getSurveyInstanceId());

                if (fresh == null || !StringUtils.hasText(fresh.getResponderUserId())) {
                    continue;
                }

                boolean needUpdate = false;

                if (fresh.getClosedAt() == null) {
                    fresh.setClosedAt(plusDaysEndOfDay(now, DEFAULT_DUE_DAYS));
                    needUpdate = true;
                }

                if (fresh.getClosedAt() != null && !fresh.getClosedAt().after(now)) {
                    fresh.setStatus("EXPIRED");
                    trySetUpdatedAt(fresh, now);
                    surveyInstanceMapper.updateByPrimaryKey(fresh);
                    continue;
                }

                if (needUpdate) {
                    trySetUpdatedAt(fresh, now);
                    surveyInstanceMapper.updateByPrimaryKey(fresh);
                }

                notifySurveyReady(fresh);

            } catch (Exception e) {
                log.error("dispatch scheduled survey failed, instanceId={}", item.getSurveyInstanceId(), e);
            }
        }
    }

    private void notifySurveyReady(SurveyInstanceEntity entity) {
        String dueStr = "";

        if (entity.getClosedAt() != null) {
            dueStr = entity.getClosedAt().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .format(DATE_FMT);
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("dueDate", dueStr);

        NotificationCreateParams params = NotificationCreateParams.builder()
                .companyId(entity.getCompanyId())
                .userId(entity.getResponderUserId())
                .type("SURVEY_READY")
                .title("New onboarding survey")
                .content("You have a new survey to complete"
                        + (StringUtils.hasText(dueStr) ? ". Please submit before " + dueStr : "."))
                .refType("SURVEY")
                .refId(entity.getSurveyInstanceId())
                .sendEmail(true)
                .emailTemplate("SURVEY_READY")
                .emailPlaceholders(placeholders)
                .onboardingId(entity.getOnboardingId())
                .build();

        notificationService.create(params);
    }

    private static Date plusDaysEndOfDay(Date start, int days) {
        LocalDate date = start.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .plusDays(days);

        LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.of(23, 59, 59));

        return Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }

    private void trySetUpdatedAt(SurveyInstanceEntity instance, Date now) {
        try {
            SurveyInstanceEntity.class
                    .getMethod("setUpdatedAt", Date.class)
                    .invoke(instance, now);
        } catch (Exception ignored) {
        }
    }
}