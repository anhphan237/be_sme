package com.sme.be_sme.modules.survey.processor;

import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyInstanceEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SurveyExpireJob {

    private final SurveyInstanceMapperExt surveyInstanceMapperExt;
    private final SurveyInstanceMapper surveyInstanceMapper;

    @Scheduled(fixedDelay = 300000)
    public void expireOverdueSurveys() {
        Date now = new Date();

        List<SurveyInstanceEntity> instances =
                surveyInstanceMapperExt.selectExpiredOpenInstances(now, 200);

        if (instances == null || instances.isEmpty()) {
            return;
        }

        for (SurveyInstanceEntity instance : instances) {
            try {
                instance.setStatus("EXPIRED");
                trySetUpdatedAt(instance, now);

                surveyInstanceMapper.updateByPrimaryKey(instance);
            } catch (Exception e) {
                log.error("expire survey failed, instanceId={}", instance.getSurveyInstanceId(), e);
            }
        }
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