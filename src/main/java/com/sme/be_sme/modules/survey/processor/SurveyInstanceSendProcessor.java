package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveySendRequest;
import com.sme.be_sme.modules.survey.api.response.SurveySendResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyInstanceEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class SurveyInstanceSendProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SurveyInstanceMapper surveyInstanceMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveySendRequest request = objectMapper.convertValue(payload, SurveySendRequest.class);
        validate(context, request);

        Date now = new Date();

        // Case A: gửi bằng surveyInstanceId -> update sent_at + status
        if (StringUtils.hasText(request.getSurveyInstanceId())) {
            SurveyInstanceEntity entity = surveyInstanceMapper.selectByPrimaryKey(request.getSurveyInstanceId().trim());
            if (entity == null || !context.getTenantId().equals(entity.getCompanyId())) {
                throw AppException.of(ErrorCodes.NOT_FOUND, "survey instance not found");
            }

            entity.setSentAt(now);
            entity.setStatus("SENT");

            int updated = surveyInstanceMapper.updateByPrimaryKey(entity);
            if (updated != 1) {
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "send survey instance failed");
            }

            SurveySendResponse res = new SurveySendResponse();
            res.setSurveyInstanceId(entity.getSurveyInstanceId());
            res.setStatus(entity.getStatus());
            res.setSentAt(entity.getSentAt());
            return res;
        }

        // Case B: gửi ngoài lịch -> tạo instance mới theo templateId (+ onboardingId nếu có)
        SurveyTemplateEntity template = surveyTemplateMapper.selectByPrimaryKey(request.getTemplateId().trim());
        if (template == null || !context.getTenantId().equals(template.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey template not found");
        }

        SurveyInstanceEntity entity = new SurveyInstanceEntity();
        entity.setSurveyInstanceId(UuidGenerator.generate());
        entity.setCompanyId(context.getTenantId());
        entity.setSurveyTemplateId(template.getSurveyTemplateId());
        entity.setOnboardingId(StringUtils.hasText(request.getOnboardingId()) ? request.getOnboardingId().trim() : null);

        entity.setScheduledAt(now); // gửi ngay
        entity.setSentAt(now);
        entity.setStatus("SENT");
        entity.setCreatedAt(now);

        int inserted = surveyInstanceMapper.insert(entity);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create & send survey instance failed");
        }

        SurveySendResponse res = new SurveySendResponse();
        res.setSurveyInstanceId(entity.getSurveyInstanceId());
        res.setStatus(entity.getStatus());
        res.setSentAt(entity.getSentAt());
        return res;
    }

    private static void validate(BizContext context, SurveySendRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        boolean hasInstanceId = StringUtils.hasText(request.getSurveyInstanceId());
        boolean hasTemplateId = StringUtils.hasText(request.getTemplateId());

        if (!hasInstanceId && !hasTemplateId) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "surveyInstanceId or templateId is required");
        }
    }
}
