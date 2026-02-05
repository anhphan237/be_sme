package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyTemplateUpdateRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyTemplateResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class SurveyTemplateUpdateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {

        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        SurveyTemplateUpdateRequest request =
                objectMapper.convertValue(payload, SurveyTemplateUpdateRequest.class);

        if (request == null || !StringUtils.hasText(request.getTemplateId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "templateId is required");
        }

        SurveyTemplateEntity entity =
                surveyTemplateMapper.selectByPrimaryKey(request.getTemplateId().trim());

        if (entity == null || !context.getTenantId().equals(entity.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey template not found");
        }

        // optional optimistic check (nếu bạn muốn)
        if (request.getVersion() != null && entity.getVersion() != null
                && !request.getVersion().equals(entity.getVersion())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "version mismatch");
        }

        // PATCH-style: field nào gửi thì update field đó
        if (StringUtils.hasText(request.getName())) {
            entity.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription()); // allow null if client wants clear
        }
        if (StringUtils.hasText(request.getStage())) {
            entity.setStage(request.getStage().trim());
        }
        if (request.getManagerOnly() != null) {
            entity.setManagerOnly(request.getManagerOnly());
        }
        if (StringUtils.hasText(request.getStatus())) {
            entity.setStatus(request.getStatus().trim());
        }
        if (request.getIsDefault() != null) {
            entity.setIsDefault(request.getIsDefault());
        }

        if (entity.getVersion() == null) entity.setVersion(1);
        else entity.setVersion(entity.getVersion() + 1);

        entity.setUpdatedAt(new Date());

        int updated = surveyTemplateMapper.updateByPrimaryKey(entity);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "update survey template failed");
        }

        SurveyTemplateResponse res = new SurveyTemplateResponse();
        res.setTemplateId(entity.getSurveyTemplateId());
        res.setName(entity.getName());
        res.setStatus(entity.getStatus());
        res.setDescription(entity.getDescription());
        res.setStage(entity.getStage());
        res.setManagerOnly(entity.getManagerOnly());
        res.setVersion(entity.getVersion());
        res.setIsDefault(entity.getIsDefault());
        return res;
    }
}
