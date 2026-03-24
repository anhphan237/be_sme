package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyResponseSaveDraftRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyResponseSaveDraftResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyResponseDraftMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyInstanceDetailRow;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class SurveyResponseSaveDraftProcessor extends BaseBizProcessor<BizContext> {

    private static final Set<String> ALLOW_STATUSES = Set.of("PENDING", "SENT", "SCHEDULED");

    private final ObjectMapper objectMapper;
    private final SurveyInstanceMapperExt surveyInstanceMapperExt;
    private final SurveyResponseDraftMapperExt surveyResponseDraftMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveyResponseSaveDraftRequest request =
                objectMapper.convertValue(payload, SurveyResponseSaveDraftRequest.class);

        String companyId = context.getTenantId();
        String operatorId = context.getOperatorId();

        SurveyInstanceDetailRow instance = surveyInstanceMapperExt.selectDetailById(
                companyId,
                request.getInstanceId(),
                operatorId
        );

        if (instance == null) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "Survey instance not found");
        }

        if (!ALLOW_STATUSES.contains(String.valueOf(instance.getStatus()).toUpperCase())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "Survey is not available for draft saving");
        }

        if (request.getAnswers() != null) {
            for (SurveyResponseSaveDraftRequest.AnswerItem answer : request.getAnswers()) {
                String answerValue;
                try {
                    answerValue = objectMapper.writeValueAsString(answer.getValue());
                } catch (Exception e) {
                    throw AppException.of(ErrorCodes.BAD_REQUEST, "Invalid answer value");
                }

                surveyResponseDraftMapperExt.upsertDraft(
                        java.util.UUID.randomUUID().toString(),
                        companyId,
                        request.getInstanceId(),
                        answer.getQuestionId(),
                        operatorId,
                        answerValue
                );
            }
        }

        return new SurveyResponseSaveDraftResponse(true);
    }
}