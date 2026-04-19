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

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SurveyMilestoneAutoCreateMapper surveyMilestoneAutoCreateMapper;
    private final NotificationService notificationService;

    /**
     * Tự động gửi survey khi employee đạt đủ mốc onboarding.
     *
     * Rule:
     * - Job chạy mỗi 2 phút.
     * - Template phải ACTIVE.
     * - Template phải is_default = true.
     * - Stage thuộc D7 / D30 / D60.
     * - target_role = EMPLOYEE thì gửi employee.
     * - target_role = MANAGER thì gửi manager_user_id của employee.
     * - Tạo survey_instance với status = SENT để user làm được ngay.
     */
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
                && StringUtils.hasText(candidate.getResponderUserId())
                && candidate.getScheduledAt() != null;
    }
}