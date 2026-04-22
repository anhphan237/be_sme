package com.sme.be_sme.modules.survey.processor;

import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyMilestoneAutoCreateMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyMilestoneCandidate;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
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
public class SurveyMilestoneAutoCreateJob {

    private static final int BATCH_SIZE = 100;
    private static final int DEFAULT_DUE_DAYS = 7;
    private static final int D60_DUE_DAYS = 14;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SurveyMilestoneAutoCreateMapper surveyMilestoneAutoCreateMapper;
    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 120000)
    public void autoSendDefaultMilestoneSurveys() {
        Date now = new Date();

        List<SurveyMilestoneCandidate> candidates =
                surveyMilestoneAutoCreateMapper.selectDefaultMilestoneCandidates(now, BATCH_SIZE);

        if (candidates == null || candidates.isEmpty()) {
            log.info("No auto milestone survey candidates found");
            return;
        }

        for (SurveyMilestoneCandidate candidate : candidates) {
            try {
                if (!isValid(candidate)) {
                    log.warn(
                            "Skip invalid milestone survey candidate, onboardingId={}, templateId={}, targetRole={}, responderUserId={}",
                            candidate == null ? null : candidate.getOnboardingId(),
                            candidate == null ? null : candidate.getTemplateId(),
                            candidate == null ? null : candidate.getTargetRole(),
                            candidate == null ? null : candidate.getResponderUserId()
                    );
                    continue;
                }

                ensureClosedAt(candidate, now);

                candidate.setSurveyInstanceId(UuidGenerator.generate());

                int inserted =
                        surveyMilestoneAutoCreateMapper.insertAutoSentSurveyInstance(candidate);

                if (inserted == 1) {
                    notifySurveyReady(candidate);

                    log.info(
                            "Auto sent milestone survey successfully, instanceId={}, onboardingId={}, templateId={}, stage={}, targetRole={}, responderUserId={}",
                            candidate.getSurveyInstanceId(),
                            candidate.getOnboardingId(),
                            candidate.getTemplateId(),
                            candidate.getStage(),
                            candidate.getTargetRole(),
                            candidate.getResponderUserId()
                    );
                } else {
                    log.info(
                            "Milestone survey already exists, onboardingId={}, templateId={}, responderUserId={}",
                            candidate.getOnboardingId(),
                            candidate.getTemplateId(),
                            candidate.getResponderUserId()
                    );
                }

            } catch (Exception e) {
                log.error(
                        "Auto send milestone survey failed, onboardingId={}, templateId={}, targetRole={}",
                        candidate == null ? null : candidate.getOnboardingId(),
                        candidate == null ? null : candidate.getTemplateId(),
                        candidate == null ? null : candidate.getTargetRole(),
                        e
                );
            }
        }
    }

    private void ensureClosedAt(SurveyMilestoneCandidate candidate, Date now) {
        if (candidate == null || candidate.getClosedAt() != null) {
            return;
        }

        int dueDays = "D60".equalsIgnoreCase(candidate.getStage())
                ? D60_DUE_DAYS
                : DEFAULT_DUE_DAYS;

        Date closedAt = plusDaysEndOfDay(now, dueDays);

        try {
            Method method = SurveyMilestoneCandidate.class.getMethod("setClosedAt", Date.class);
            method.invoke(candidate, closedAt);
        } catch (Exception e) {
            log.warn(
                    "Cannot set closedAt on milestone candidate, onboardingId={}, templateId={}",
                    candidate.getOnboardingId(),
                    candidate.getTemplateId()
            );
        }
    }

    private void notifySurveyReady(SurveyMilestoneCandidate candidate) {
        if (candidate == null || !StringUtils.hasText(candidate.getResponderUserId())) {
            return;
        }

        String dueStr = "";
        if (candidate.getClosedAt() != null) {
            dueStr = candidate.getClosedAt()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .format(DATE_FMT);
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("dueDate", dueStr);
        placeholders.put(
                "templateName",
                StringUtils.hasText(candidate.getTemplateName())
                        ? candidate.getTemplateName()
                        : ""
        );

        NotificationCreateParams params = NotificationCreateParams.builder()
                .companyId(candidate.getCompanyId())
                .userId(candidate.getResponderUserId())
                .type("SURVEY_READY")
                .title("New onboarding survey")
                .content("You have a new survey to complete"
                        + (StringUtils.hasText(dueStr)
                        ? ". Please submit before " + dueStr
                        : "."))
                .refType("SURVEY")
                .refId(candidate.getSurveyInstanceId())
                .sendEmail(true)
                .emailTemplate("SURVEY_READY")
                .emailPlaceholders(placeholders)
                .onboardingId(candidate.getOnboardingId())
                .build();

        notificationService.create(params);
    }

    private boolean isValid(SurveyMilestoneCandidate candidate) {
        return candidate != null
                && StringUtils.hasText(candidate.getCompanyId())
                && StringUtils.hasText(candidate.getOnboardingId())
                && StringUtils.hasText(candidate.getTemplateId())
                && StringUtils.hasText(candidate.getTargetRole())
                && isValidTargetRole(candidate.getTargetRole())
                && StringUtils.hasText(candidate.getResponderUserId())
                && candidate.getScheduledAt() != null;
    }

    private boolean isValidTargetRole(String targetRole) {
        return "EMPLOYEE".equalsIgnoreCase(targetRole)
                || "MANAGER".equalsIgnoreCase(targetRole);
    }

    private static Date plusDaysEndOfDay(Date start, int days) {
        LocalDate date = start.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .plusDays(days);

        LocalDateTime endOfDay = LocalDateTime.of(date, LocalTime.of(23, 59, 59));

        return Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }
}