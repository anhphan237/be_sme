package com.sme.be_sme.modules.survey.processor;

import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyMilestoneAutoCreateMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyMilestoneCandidate;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SurveyMilestoneAutoCreateJob {

    private static final int BATCH_SIZE = 100;

    private final SurveyMilestoneAutoCreateMapper surveyMilestoneAutoCreateMapper;

    @Scheduled(fixedDelay = 300000)
    public void autoCreateDefaultMilestoneSurveyInstances() {
        Date now = new Date();

        List<SurveyMilestoneCandidate> candidates =
                surveyMilestoneAutoCreateMapper.selectDefaultMilestoneCandidates(now, BATCH_SIZE);

        if (candidates == null || candidates.isEmpty()) {
            return;
        }

        for (SurveyMilestoneCandidate candidate : candidates) {
            try {
                if (!isValid(candidate)) {
                    log.warn(
                            "Skip invalid auto survey candidate, onboardingId={}, templateId={}, targetRole={}, responderUserId={}",
                            candidate == null ? null : candidate.getOnboardingId(),
                            candidate == null ? null : candidate.getTemplateId(),
                            candidate == null ? null : candidate.getTargetRole(),
                            candidate == null ? null : candidate.getResponderUserId()
                    );
                    continue;
                }

                candidate.setSurveyInstanceId(UuidGenerator.generate());

                int inserted = surveyMilestoneAutoCreateMapper.insertAutoSurveyInstance(candidate);

                if (inserted == 1) {
                    log.info(
                            "Auto-created survey instance, instanceId={}, onboardingId={}, templateId={}, stage={}, targetRole={}, responderUserId={}",
                            candidate.getSurveyInstanceId(),
                            candidate.getOnboardingId(),
                            candidate.getTemplateId(),
                            candidate.getStage(),
                            candidate.getTargetRole(),
                            candidate.getResponderUserId()
                    );
                }

            } catch (Exception e) {
                log.error(
                        "Auto-create survey instance failed, onboardingId={}, templateId={}, targetRole={}",
                        candidate == null ? null : candidate.getOnboardingId(),
                        candidate == null ? null : candidate.getTemplateId(),
                        candidate == null ? null : candidate.getTargetRole(),
                        e
                );
            }
        }
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