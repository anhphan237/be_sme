package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyQuestionCreateRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyQuestionResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyQuestionMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyQuestionEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class SurveyQuestionCreateProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;
    private final SurveyQuestionMapper surveyQuestionMapper;
    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveyQuestionCreateRequest request =
                objectMapper.convertValue(payload, SurveyQuestionCreateRequest.class);
        validate(context, request);


        SurveyTemplateEntity template =
                surveyTemplateMapper.selectByPrimaryKey(request.getTemplateId().trim());

        if (template == null || !context.getTenantId().equals(template.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey template not found");
        }


        Date now = new Date();
        String questionId = UuidGenerator.generate();

        SurveyQuestionEntity q = new SurveyQuestionEntity();
        q.setSurveyQuestionId(questionId);
        q.setCompanyId(context.getTenantId());
        q.setSurveyTemplateId(request.getTemplateId().trim());
        q.setType(request.getType().trim());
        q.setContent(request.getContent().trim());
        q.setRequired(request.getRequired() != null ? request.getRequired() : Boolean.FALSE);
        q.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        if (request.getOptionsJson() != null && !request.getOptionsJson().isEmpty()) {
            q.setOptionsJson(request.getOptionsJson().toString());
        } else {
            q.setOptionsJson(null);
        }
        q.setDimensionCode(request.getDimensionCode());
        q.setMeasurable(request.getMeasurable() != null ? request.getMeasurable() : Boolean.TRUE);
        q.setScaleMin(request.getScaleMin() != null ? request.getScaleMin() : 1);
        q.setScaleMax(request.getScaleMax() != null ? request.getScaleMax() : 5);
        q.setCreatedAt(now);
        q.setUpdatedAt(now);

        int inserted = surveyQuestionMapper.insert(q);
        if (inserted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "create survey question failed");
        }

        SurveyQuestionResponse res = new SurveyQuestionResponse();
        res.setQuestionId(questionId);
        res.setTemplateId(q.getSurveyTemplateId());
        res.setRequired(q.getRequired());
        res.setSortOrder(q.getSortOrder());
        res.setDimensionCode(q.getDimensionCode());
        res.setMeasurable(q.getMeasurable());
        res.setScaleMin(q.getScaleMin());
        res.setScaleMax(q.getScaleMax());
        res.setDimensionCode(q.getDimensionCode());
        res.setType(q.getType());
        res.setContent(q.getContent());
        Object opt = q.getOptionsJson();
        res.setOptionsJson(opt == null ? null : opt.toString());
        return res;
    }
    private static void validate(BizContext context, SurveyQuestionCreateRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getTemplateId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "templateId is required");
        }
        if (!StringUtils.hasText(request.getType())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "type is required");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "content is required");
        }

        String type = request.getType().trim();
        if (!type.equals("RATING") && !type.equals("TEXT") && !type.equals("CHOICE")) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "type must be RATING|TEXT|CHOICE");
        }

    }
}
