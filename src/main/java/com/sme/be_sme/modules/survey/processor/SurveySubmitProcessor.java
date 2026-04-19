package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapper;
import com.sme.be_sme.modules.identity.infrastructure.mapper.UserMapperExt;
import com.sme.be_sme.modules.identity.infrastructure.persistence.entity.UserEntity;
import com.sme.be_sme.modules.notification.service.NotificationCreateParams;
import com.sme.be_sme.modules.notification.service.NotificationService;
import com.sme.be_sme.modules.survey.api.request.SurveySubmitRequest;
import com.sme.be_sme.modules.survey.api.response.SurveySubmitResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyAnswerMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyQuestionMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyResponseDraftMapperExt;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SurveySubmitProcessor extends BaseBizProcessor<BizContext> {

    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String STATUS_PENDING = "PENDING";

    private final ObjectMapper objectMapper;
    private final SurveyInstanceMapper surveyInstanceMapper;
    private final SurveyTemplateMapper surveyTemplateMapper;
    private final SurveyQuestionMapper surveyQuestionMapper;
    private final SurveyResponseMapper surveyResponseMapper;
    private final SurveyAnswerMapper surveyAnswerMapper;
    private final SurveyResponseDraftMapperExt surveyResponseDraftMapperExt;
    private final UserMapperExt userMapperExt;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveySubmitRequest request = objectMapper.convertValue(payload, SurveySubmitRequest.class);
        validate(context, request);

        Date now = new Date();
        String tenantId = context.getTenantId();
        String operatorId = context.getOperatorId();

        SurveyInstanceEntity instance =
                surveyInstanceMapper.selectByPrimaryKey(request.getSurveyInstanceId().trim());

        if (instance == null || !tenantId.equals(instance.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey instance not found");
        }
        if (instance.getScheduledAt() != null
                && instance.getScheduledAt().toInstant().isAfter(now.toInstant())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "Survey is not open yet");
        }

        if (STATUS_COMPLETED.equalsIgnoreCase(instance.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "survey already submitted");
        }

        if (StringUtils.hasText(instance.getResponderUserId())
                && !instance.getResponderUserId().equals(context.getOperatorId())) {
            throw AppException.of(ErrorCodes.FORBIDDEN, "You are not allowed to submit this survey");
        }

        openScheduledSurveyIfDue(instance, now);

        if (!STATUS_SENT.equalsIgnoreCase(instance.getStatus())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "survey is not available for submission");
        }
        SurveyTemplateEntity template =
                surveyTemplateMapper.selectByPrimaryKey(instance.getSurveyTemplateId());

        if (template == null || !tenantId.equals(template.getCompanyId())) {
            throw AppException.of(ErrorCodes.NOT_FOUND, "survey template not found");
        }

        List<SurveyQuestionEntity> questions =
                filterQuestions(tenantId, template.getSurveyTemplateId());

        if (questions.isEmpty()) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "survey template has no questions");
        }

        Map<String, String> answers = normalizeAnswers(request.getAnswers());
        ensureRequiredAnswered(questions, answers);
        validateAnswersByType(questions, answers);

        SurveyResponseEntity responseEntity = new SurveyResponseEntity();
        responseEntity.setSurveyResponseId(UuidGenerator.generate());
        responseEntity.setCompanyId(tenantId);
        responseEntity.setSurveyInstanceId(instance.getSurveyInstanceId());
        responseEntity.setResponderUserId(StringUtils.hasText(operatorId) ? operatorId : "system");
        responseEntity.setSubmittedAt(now);
        responseEntity.setCreatedAt(now);
        responseEntity.setOverallScore(calculateOverallScore(questions, answers));

        int insertedResponse = surveyResponseMapper.insert(responseEntity);
        if (insertedResponse != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "submit survey response failed");
        }

        List<SurveyAnswerEntity> answerEntities = buildAnswerEntities(
                tenantId,
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

        instance.setStatus(STATUS_COMPLETED);
        instance.setClosedAt(now);
        trySetUpdatedAt(instance, now);

        int updatedInstance = surveyInstanceMapper.updateByPrimaryKey(instance);
        if (updatedInstance != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "update survey instance status failed");
        }

        if (StringUtils.hasText(operatorId)) {
            surveyResponseDraftMapperExt.deleteByInstanceIdAndResponder(
                    tenantId,
                    instance.getSurveyInstanceId(),
                    operatorId
            );
        }

        notifyHrWhenSubmitted(tenantId, operatorId, instance, template);

        SurveySubmitResponse response = new SurveySubmitResponse();
        response.setSurveyInstanceId(instance.getSurveyInstanceId());
        response.setStatus(STATUS_COMPLETED);
        return response;
    }
    private void openScheduledSurveyIfDue(SurveyInstanceEntity instance, Date now) {
        if (instance == null || !StringUtils.hasText(instance.getStatus())) {
            return;
        }

        String status = instance.getStatus().trim().toUpperCase(Locale.US);

        boolean canAutoOpen =
                STATUS_SCHEDULED.equals(status)
                        || STATUS_PENDING.equals(status);

        if (!canAutoOpen) {
            return;
        }

        if (instance.getScheduledAt() != null
                && instance.getScheduledAt().toInstant().isAfter(now.toInstant())) {
            return;
        }

        if (instance.getClosedAt() != null
                && !instance.getClosedAt().after(now)) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "survey is expired");
        }

        instance.setStatus(STATUS_SENT);

        if (instance.getSentAt() == null) {
            instance.setSentAt(now);
        }

        trySetUpdatedAt(instance, now);

        int updated = surveyInstanceMapper.updateByPrimaryKey(instance);
        if (updated != 1) {
            throw AppException.of(ErrorCodes.INTERNAL_ERROR, "open scheduled survey failed");
        }
    }
    private void notifyHrWhenSubmitted(
            String tenantId,
            String operatorId,
            SurveyInstanceEntity instance,
            SurveyTemplateEntity template
    ) {
        List<String> hrUserIds = userMapperExt.selectHrUserIdsByCompanyId(tenantId);
        if (hrUserIds == null || hrUserIds.isEmpty()) {
            return;
        }

        String responderName = "A user";
        if (StringUtils.hasText(operatorId)) {
            UserEntity responder = userMapper.selectByPrimaryKey(operatorId);
            if (responder != null && StringUtils.hasText(responder.getFullName())) {
                responderName = responder.getFullName();
            }
        }

        String templateName = template != null ? template.getName() : "";

        for (String hrUserId : hrUserIds) {
            if (!StringUtils.hasText(hrUserId)) {
                continue;
            }

            NotificationCreateParams params = NotificationCreateParams.builder()
                    .companyId(tenantId)
                    .userId(hrUserId)
                    .type("SURVEY_SUBMITTED")
                    .title("Survey submitted")
                    .content(responderName + " has submitted survey"
                            + (StringUtils.hasText(templateName) ? ": " + templateName : "."))
                    .refType("SURVEY")
                    .refId(instance.getSurveyInstanceId())
                    .sendEmail(false)
                    .onboardingId(instance.getOnboardingId())
                    .build();

            notificationService.create(params);
        }
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
        List<SurveyQuestionEntity> questions = surveyQuestionMapper.selectByTemplateId(templateId);
        List<SurveyQuestionEntity> filtered = new ArrayList<>();
        if (questions == null) {
            return filtered;
        }
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

    private Map<String, String> normalizeAnswers(List<SurveySubmitRequest.AnswerItem> answers) {
        Map<String, String> result = new HashMap<>();

        for (SurveySubmitRequest.AnswerItem item : answers) {
            if (item == null || !StringUtils.hasText(item.getQuestionId())) {
                continue;
            }

            Object value = item.getValue();
            String normalizedValue;

            if (value == null) {
                normalizedValue = null;
            } else if (value instanceof List<?> listValue) {
                normalizedValue = toJson(listValue);
            } else {
                normalizedValue = String.valueOf(value);
            }

            result.put(item.getQuestionId(), normalizedValue);
        }

        return result;
    }

    private static void ensureRequiredAnswered(
            List<SurveyQuestionEntity> questions,
            Map<String, String> answers
    ) {
        for (SurveyQuestionEntity question : questions) {
            if (question == null || Boolean.FALSE.equals(question.getRequired())) {
                continue;
            }
            String answer = answers.get(question.getSurveyQuestionId());
            if (!StringUtils.hasText(answer)) {
                throw AppException.of(
                        ErrorCodes.BAD_REQUEST,
                        "missing answer for question " + question.getSurveyQuestionId()
                );
            }
        }
    }

    private void validateAnswersByType(
            List<SurveyQuestionEntity> questions,
            Map<String, String> answers
    ) {
        for (SurveyQuestionEntity question : questions) {
            if (question == null) {
                continue;
            }

            String raw = answers.get(question.getSurveyQuestionId());
            if (!StringUtils.hasText(raw)) {
                continue;
            }

            String type = question.getType() == null ? "" : question.getType().trim().toUpperCase(Locale.US);

            switch (type) {
                case "RATING" -> validateRatingAnswer(question, raw);
                case "CHOICE", "SINGLE_CHOICE" -> validateSingleChoiceAnswer(question, raw);
                case "MULTIPLE_CHOICE", "MULTI_CHOICE", "CHECKBOX" -> validateMultipleChoiceAnswer(question, raw);
                default -> {
                }
            }
        }
    }

    private void validateRatingAnswer(SurveyQuestionEntity question, String raw) {
        Integer rating = parseInteger(raw.trim());
        if (rating == null) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid rating answer");
        }

        Integer min = question.getScaleMin() != null ? question.getScaleMin() : 1;
        Integer max = question.getScaleMax() != null ? question.getScaleMax() : 5;

        if (rating < min || rating > max) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "rating answer is out of range");
        }
    }

    private void validateSingleChoiceAnswer(SurveyQuestionEntity question, String raw) {
        Set<String> validOptions = parseQuestionOptions(question);
        if (!validOptions.isEmpty() && !validOptions.contains(raw.trim())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid choice answer");
        }
    }

    private void validateMultipleChoiceAnswer(SurveyQuestionEntity question, String raw) {
        Set<String> validOptions = parseQuestionOptions(question);
        List<String> selectedValues = parseSubmittedValues(raw);

        if (selectedValues.isEmpty() && Boolean.TRUE.equals(question.getRequired())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid multiple choice answer");
        }

        for (String value : selectedValues) {
            if (!validOptions.isEmpty() && !validOptions.contains(value)) {
                throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid multiple choice answer");
            }
        }
    }

    private Set<String> parseQuestionOptions(SurveyQuestionEntity question) {
        Set<String> options = new LinkedHashSet<>();
        if (question == null || question.getOptionsJson() == null) {
            return options;
        }

        try {
            String raw = String.valueOf(question.getOptionsJson());
            List<String> values = objectMapper.readValue(raw, new TypeReference<List<String>>() {});
            for (String value : values) {
                if (StringUtils.hasText(value)) {
                    options.add(value.trim());
                }
            }
        } catch (Exception ignored) {
        }

        return options;
    }

    private List<String> parseSubmittedValues(String raw) {
        List<String> result = new ArrayList<>();
        if (!StringUtils.hasText(raw)) {
            return result;
        }

        String trimmed = raw.trim();
        try {
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                List<String> values = objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
                for (String value : values) {
                    if (StringUtils.hasText(value)) {
                        result.add(value.trim());
                    }
                }
                return result;
            }
        } catch (Exception ignored) {
        }

        for (String value : trimmed.split(",")) {
            if (StringUtils.hasText(value)) {
                result.add(value.trim().replace("\"", ""));
            }
        }
        return result;
    }

    private static BigDecimal calculateOverallScore(
            List<SurveyQuestionEntity> questions,
            Map<String, String> answers
    ) {
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

        return BigDecimal.valueOf((double) ratingSum / ratingCount)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<SurveyAnswerEntity> buildAnswerEntities(
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

    private static void applyAnswerByType(
            SurveyAnswerEntity answerEntity,
            String type,
            String answer
    ) {
        if (!StringUtils.hasText(type)) {
            answerEntity.setValueText(answer);
            return;
        }

        String normalizedType = type.trim().toUpperCase(Locale.US);

        switch (normalizedType) {
            case "RATING" -> answerEntity.setValueRating(parseInteger(answer));
            case "CHOICE", "SINGLE_CHOICE" -> answerEntity.setValueChoice(answer);
            case "MULTIPLE_CHOICE", "MULTI_CHOICE", "CHECKBOX" -> answerEntity.setValueText(answer);
            case "TEXT" -> answerEntity.setValueText(answer);
            default -> answerEntity.setValueText(answer);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "invalid answer format");
        }
    }

    private void trySetUpdatedAt(SurveyInstanceEntity instance, Date now) {
        try {
            SurveyInstanceEntity.class
                    .getMethod("setUpdatedAt", Date.class)
                    .invoke(instance, now);
        } catch (Exception ignored) {
        }
    }
}