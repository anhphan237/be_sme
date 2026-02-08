package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyQuestionDeleteRequest;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyQuestionMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyQuestionEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class SurveyQuestionDeleteProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SurveyQuestionMapper surveyQuestionMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {

        SurveyQuestionDeleteRequest request =
                objectMapper.convertValue(payload, SurveyQuestionDeleteRequest.class);

        if (context == null || context.getTenantId() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null || request.getQuestionId() == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "questionId is required");
        }

        SurveyQuestionEntity q = surveyQuestionMapper.selectByPrimaryKey(request.getQuestionId());
        if (q == null || !context.getTenantId().equals(q.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey question not found");
        }

        int deleted = surveyQuestionMapper.deleteByPrimaryKey(request.getQuestionId());
        if (deleted != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "delete survey question failed");
        }

        return Collections.singletonMap("deleted", true);
    }
}

