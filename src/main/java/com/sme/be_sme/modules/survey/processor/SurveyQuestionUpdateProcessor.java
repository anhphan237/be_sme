package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyQuestionUpdateRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyQuestionUpdateResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyQuestionMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyQuestionEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class SurveyQuestionUpdateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SurveyQuestionMapper surveyQuestionMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {

        SurveyQuestionUpdateRequest req =
                objectMapper.convertValue(payload, SurveyQuestionUpdateRequest.class);

        validate(context, req);

        SurveyQuestionEntity existed = surveyQuestionMapper.selectByPrimaryKey(req.getQuestionId());
        if (existed == null || !context.getTenantId().equals(existed.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey question not found");
        }

        // Update only fields you allow (giữ templateId/type như cũ)
        if (StringUtils.hasText(req.getContent())) existed.setContent(req.getContent().trim());
        if (req.getRequired() != null) existed.setRequired(req.getRequired());
        if (req.getSortOrder() != null) existed.setSortOrder(req.getSortOrder());

        if (req.getDimensionCode() != null) existed.setDimensionCode(req.getDimensionCode());
        if (req.getMeasurable() != null) existed.setMeasurable(req.getMeasurable());
        if (req.getScaleMin() != null) existed.setScaleMin(req.getScaleMin());
        if (req.getScaleMax() != null) existed.setScaleMax(req.getScaleMax());

        // optionsJson: nên là JSON STRING để insert/update vào jsonb ổn định
        if (req.getOptionsJson() != null) existed.setOptionsJson(req.getOptionsJson());

        existed.setUpdatedAt(new Date());

        int updated = surveyQuestionMapper.updateByPrimaryKey(existed);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "update survey question failed");
        }

        SurveyQuestionUpdateResponse res = new SurveyQuestionUpdateResponse();
        res.setQuestionId(existed.getSurveyQuestionId());
        res.setTemplateId(existed.getSurveyTemplateId());
        res.setType(existed.getType());
        res.setContent(existed.getContent());
        res.setRequired(existed.getRequired());
        res.setSortOrder(existed.getSortOrder());
        res.setDimensionCode(existed.getDimensionCode());
        res.setMeasurable(existed.getMeasurable());
        res.setScaleMin(existed.getScaleMin());
        res.setScaleMax(existed.getScaleMax());
        res.setOptionsJson((String) existed.getOptionsJson());
        return res;
    }

    private static void validate(BizContext context, SurveyQuestionUpdateRequest req) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (req == null || !StringUtils.hasText(req.getQuestionId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "questionId is required");
        }
    }
}
