package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyScheduleRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyScheduleResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyInstanceEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class SurveyScheduleProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;
    private final SurveyInstanceMapper surveyInstanceMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveyScheduleRequest request =
                objectMapper.convertValue(payload, SurveyScheduleRequest.class);
        validate(context, request);

        SurveyTemplateEntity template =
                surveyTemplateMapper.selectByPrimaryKey(request.getTemplateId().trim());

        if (template == null || !context.getTenantId().equals(template.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey template not found");
        }

        Date now = new Date();

        Date baseDate = request.getJoinDate() != null
                ? request.getJoinDate()
                : now;


        Date scheduledAt = plusDays(baseDate, request.getMilestoneDays());

        int dueDays = request.getDueDays() != null ? request.getDueDays() : 3;
        Date closedAt = plusDays(scheduledAt, dueDays);

        SurveyInstanceEntity entity = new SurveyInstanceEntity();
        entity.setSurveyInstanceId(UuidGenerator.generate());
        entity.setCompanyId(context.getTenantId());
        entity.setOnboardingId(request.getOnboardingId());
        entity.setSurveyTemplateId(template.getSurveyTemplateId());

        entity.setScheduledAt(scheduledAt); // openAt
        entity.setClosedAt(closedAt);       // dueAt
        entity.setStatus("SCHEDULED");
        entity.setCreatedAt(now);

        int inserted = surveyInstanceMapper.insert(entity);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "schedule survey instance failed");
        }

        SurveyScheduleResponse response = new SurveyScheduleResponse();
        response.setScheduleId(entity.getSurveyInstanceId());
        response.setStatus(entity.getStatus());
        return response;
    }

    private static Date plusDays(Date start, int days) {
        return new Date(start.getTime() + (long) days * 24 * 60 * 60 * 1000);
    }
    private static void validate(BizContext context, SurveyScheduleRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getTemplateId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "templateId is required");
        }
        if (!StringUtils.hasText(request.getOnboardingId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "onboardingId is required");
        }
        if (request.getMilestoneDays() == null || request.getMilestoneDays() < 0) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "milestoneDays must be >= 0");
        }
    }

}
