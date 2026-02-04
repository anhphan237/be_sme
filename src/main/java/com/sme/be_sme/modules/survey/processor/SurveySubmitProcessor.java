package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveySubmitRequest;
import com.sme.be_sme.modules.survey.api.response.SurveySubmitResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyAnswerMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyQuestionMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyResponseMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyTemplateMapper;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyAnswerEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyInstanceEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyQuestionEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyResponseEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyTemplateEntity;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import com.sme.be_sme.shared.util.UuidGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class SurveySubmitProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SurveyInstanceMapper surveyInstanceMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;
    private final SurveyQuestionMapper surveyQuestionMapper;
    private final SurveyResponseMapper surveyResponseMapper;
    private final SurveyAnswerMapper surveyAnswerMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveySubmitRequest request = objectMapper.convertValue(payload, SurveySubmitRequest.class);
        validate(context, request);

        SurveyInstanceEntity instance = surveyInstanceMapper.selectByPrimaryKey(request.getSurveyInstanceId().trim());
        if (instance == null || !context.getTenantId().equals(instance.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey instance not found");
        }

        SurveyTemplateEntity template = surveyTemplateMapper.selectByPrimaryKey(instance.getSurveyTemplateId());
        if (template == null || !context.getTenantId().equals(template.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey template not found");
        }

        List<SurveyQuestionEntity> questions = filterQuestions(context.getTenantId(), template.getSurveyTemplateId());
        if (questions.isEmpty()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "survey template has no questions");
        }

        Map<String, String> answers = normalizeAnswers(request.getAnswers());
        ensureRequiredAnswered(questions, answers);

        Date now = new Date();
        SurveyResponseEntity responseEntity = new SurveyResponseEntity();
        responseEntity.setSurveyResponseId(UuidGenerator.generate());
        responseEntity.setCompanyId(context.getTenantId());
        responseEntity.setSurveyInstanceId(instance.getSurveyInstanceId());
        responseEntity.setResponderUserId("system");
        responseEntity.setSubmittedAt(now);
        responseEntity.setCreatedAt(now);

        BigDecimal overallScore = calculateOverallScore(questions, answers);
        responseEntity.setOverallScore(overallScore);

        int insertedResponse = surveyResponseMapper.insert(responseEntity);
        if (insertedResponse != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "submit survey response failed");
        }

        List<SurveyAnswerEntity> answerEntities = buildAnswerEntities(
                context.getTenantId(),
                responseEntity.getSurveyResponseId(),
                questions,
                answers,
                now
        );

        for (SurveyAnswerEntity answerEntity : answerEntities) {
            int insertedAnswer = surveyAnswerMapper.insert(answerEntity);
            if (insertedAnswer != 1) {
                throw AppException.of(ErrorCodes.INTERNAL_ERROR, "submit survey answer failed");
            }
        }

        SurveySubmitResponse response = new SurveySubmitResponse();
        response.setSurveyInstanceId(instance.getSurveyInstanceId());
        response.setStatus("SUBMITTED");
        return response;
    }

    private static void validate(BizContext context, SurveySubmitRequest request) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
        if (request == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "payload is required");
        }
        if (!StringUtils.hasText(request.getSurveyInstanceId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "surveyInstanceId is required");
        }
        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "answers is required");
        }
    }

    private List<SurveyQuestionEntity> filterQuestions(String tenantId, String templateId) {
        List<SurveyQuestionEntity> questions = surveyQuestionMapper.selectAll();
        List<SurveyQuestionEntity> filtered = new ArrayList<>();
        for (SurveyQuestionEntity question : questions) {
            if (question == null) {
                continue;
            }
            if (!tenantId.equals(question.getCompanyId())) {
                continue;
            }
            if (!templateId.equals(question.getSurveyTemplateId())) {
                continue;
            }
            filtered.add(question);
        }
        return filtered;
    }

    private static Map<String, String> normalizeAnswers(Map<String, String> answers) {
        return answers;
    }

    private static void ensureRequiredAnswered(List<SurveyQuestionEntity> questions, Map<String, String> answers) {
        for (SurveyQuestionEntity question : questions) {
            if (question == null || Boolean.FALSE.equals(question.getRequired())) {
                continue;
            }
            String answer = answers.get(question.getSurveyQuestionId());
            if (!StringUtils.hasText(answer)) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "missing answer for question " + question.getSurveyQuestionId());
            }
        }
    }

    private static BigDecimal calculateOverallScore(List<SurveyQuestionEntity> questions, Map<String, String> answers) {
        int ratingCount = 0;
        int ratingSum = 0;
        for (SurveyQuestionEntity question : questions) {
            if (question == null) {
                continue;
            }
            if (!"RATING".equalsIgnoreCase(question.getType())) {
                continue;
            }
            String raw = answers.get(question.getSurveyQuestionId());
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            Integer rating = parseInteger(raw.trim());
            if (rating == null) {
                continue;
            }
            ratingCount++;
            ratingSum += rating;
        }
        if (ratingCount == 0) {
            return null;
        }
        return BigDecimal.valueOf((double) ratingSum / ratingCount).setScale(2, RoundingMode.HALF_UP);
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static List<SurveyAnswerEntity> buildAnswerEntities(
            String tenantId,
            String surveyResponseId,
            List<SurveyQuestionEntity> questions,
            Map<String, String> answers,
            Date now
    ) {
        List<SurveyAnswerEntity> answerEntities = new ArrayList<>();
        for (SurveyQuestionEntity question : questions) {
            if (question == null) {
                continue;
            }
            String rawAnswer = answers.get(question.getSurveyQuestionId());
            if (!StringUtils.hasText(rawAnswer)) {
                continue;
            }
            String trimmedAnswer = rawAnswer.trim();
            SurveyAnswerEntity answerEntity = new SurveyAnswerEntity();
            answerEntity.setSurveyAnswerId(UuidGenerator.generate());
            answerEntity.setCompanyId(tenantId);
            answerEntity.setSurveyResponseId(surveyResponseId);
            answerEntity.setSurveyQuestionId(question.getSurveyQuestionId());
            answerEntity.setCreatedAt(now);
            applyAnswerByType(answerEntity, question.getType(), trimmedAnswer);
            answerEntities.add(answerEntity);
        }
        return answerEntities;
    }

    private static void applyAnswerByType(SurveyAnswerEntity answerEntity, String type, String answer) {
        if (!StringUtils.hasText(type)) {
            answerEntity.setValueText(answer);
            return;
        }
        String normalizedType = type.trim().toUpperCase(Locale.US);
        switch (normalizedType) {
            case "RATING" -> answerEntity.setValueRating(parseInteger(answer));
            case "CHOICE" -> answerEntity.setValueChoice(answer);
            case "TEXT" -> answerEntity.setValueText(answer);
            default -> answerEntity.setValueText(answer);
        }
    }
}
