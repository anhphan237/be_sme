package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyQuestionGetByTemplateRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyQuestionListResponse;
import com.sme.be_sme.modules.survey.api.response.SurveyQuestionResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyQuestionMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyQuestionEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SurveyQuestionGetByTemplateProcessor
        extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;
    private final SurveyQuestionMapper surveyQuestionMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {

        if (context == null || context.getTenantId() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        SurveyQuestionGetByTemplateRequest request =
                objectMapper.convertValue(payload, SurveyQuestionGetByTemplateRequest.class);

        if (request == null || request.getTemplateId() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "templateId is required");
        }


        SurveyTemplateEntity template =
                surveyTemplateMapper.selectByPrimaryKey(request.getTemplateId());

        if (template == null ||
                !context.getTenantId().equals(template.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey template not found");
        }


        List<SurveyQuestionEntity> entities =
                surveyQuestionMapper.selectByTemplateId(template.getSurveyTemplateId());

        List<SurveyQuestionResponse> questions = new ArrayList<>();

        for (SurveyQuestionEntity q : entities) {
            SurveyQuestionResponse dto = new SurveyQuestionResponse();
            dto.setQuestionId(q.getSurveyQuestionId());
            dto.setType(q.getType());
            dto.setContent(q.getContent());
            dto.setRequired(q.getRequired());
            dto.setSortOrder(q.getSortOrder());
            dto.setDimensionCode(q.getDimensionCode());
            dto.setMeasurable(q.getMeasurable());
            dto.setScaleMin(q.getScaleMin());
            dto.setScaleMax(q.getScaleMax());
            dto.setTemplateId(q.getSurveyTemplateId());
            questions.add(dto);
        }

        SurveyQuestionListResponse res = new SurveyQuestionListResponse();
        res.setTemplateId(template.getSurveyTemplateId());
        res.setQuestions(questions);
        return res;
    }
}
