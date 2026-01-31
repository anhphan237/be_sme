package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyTemplateCreateRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyTemplateResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapper;
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
public class SurveyTemplateCreateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveyTemplateCreateRequest request = objectMapper.convertValue(payload, SurveyTemplateCreateRequest.class);
        validate(context, request);

        Date now = new Date();
        String templateId = UuidGenerator.generate();

        SurveyTemplateEntity entity = new SurveyTemplateEntity();
        entity.setSurveyTemplateId(templateId);
        entity.setCompanyId(context.getTenantId());
        entity.setName(request.getName().trim());
        entity.setStage("D7");
        entity.setManagerOnly(Boolean.FALSE);
        entity.setStatus("DRAFT");
        entity.setCreatedBy("system");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        int inserted = surveyTemplateMapper.insert(entity);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create survey template failed");
        }

        SurveyTemplateResponse response = new SurveyTemplateResponse();
        response.setTemplateId(templateId);
        response.setName(entity.getName());
        response.setStatus(entity.getStatus());
        return response;
    }

    private static void validate(BizContext context, SurveyTemplateCreateRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is required");
        }
        if (request.getName().trim().length() > 255) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "name is too long");
        }
    }
}
