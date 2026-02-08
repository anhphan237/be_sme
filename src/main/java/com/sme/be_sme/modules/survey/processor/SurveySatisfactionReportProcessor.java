package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveySatisfactionReportRequest;
import com.sme.be_sme.modules.survey.api.response.SurveySatisfactionReportResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyAnswerMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyQuestionMapper;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyResponseMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyAnswerEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.entity.SurveyQuestionEntity;
import com.sme.be_sme.modules.survey.infrastructure.persistence.model.SurveyResponseFilterRow;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SurveySatisfactionReportProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SurveyResponseMapperExt surveyResponseMapperExt;
    private final SurveyAnswerMapperExt surveyAnswerMapperExt;
    private final SurveyQuestionMapper surveyQuestionMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        SurveySatisfactionReportRequest request = payload != null && !payload.isNull()
                ? objectMapper.convertValue(payload, SurveySatisfactionReportRequest.class)
                : new SurveySatisfactionReportRequest();
        validate(context);

        String companyId = context.getTenantId();
        String stageStr = request.getStage() != null ? String.valueOf(request.getStage()) : null;

        List<SurveyResponseFilterRow> rows = surveyResponseMapperExt.selectByCompanyIdAndFilters(
                companyId,
                request.getStartDate(),
                request.getEndDate(),
                request.getTemplateId(),
                stageStr
        );
        if (rows == null || rows.isEmpty()) {
            SurveySatisfactionReportResponse empty = new SurveySatisfactionReportResponse();
            empty.setOverallSatisfactionScore(null);
            empty.setQuestionStats(Collections.emptyList());
            return empty;
        }

        List<String> responseIds = rows.stream()
                .map(SurveyResponseFilterRow::getSurveyResponseId)
                .distinct()
                .collect(Collectors.toList());

        BigDecimal overallSatisfactionScore = computeOverallScore(rows);

        List<SurveyAnswerEntity> answers = surveyAnswerMapperExt.selectByCompanyIdAndResponseIds(companyId, responseIds);
        if (answers == null) {
            answers = Collections.emptyList();
        }

        Set<String> templateIds = rows.stream()
                .map(SurveyResponseFilterRow::getSurveyTemplateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, SurveyQuestionEntity> questionMap = new HashMap<>();
        for (String templateId : templateIds) {
            List<SurveyQuestionEntity> qs = surveyQuestionMapper.selectByTemplateId(templateId);
            if (qs != null) {
                for (SurveyQuestionEntity q : qs) {
                    questionMap.putIfAbsent(q.getSurveyQuestionId(), q);
                }
            }
        }

        List<SurveySatisfactionReportResponse.QuestionStat> questionStats = buildQuestionStats(questionMap, answers);
        questionStats.sort(Comparator.comparing(SurveySatisfactionReportResponse.QuestionStat::getQuestionId));

        SurveySatisfactionReportResponse response = new SurveySatisfactionReportResponse();
        response.setOverallSatisfactionScore(overallSatisfactionScore);
        response.setQuestionStats(questionStats);
        return response;
    }

    private static BigDecimal computeOverallScore(List<SurveyResponseFilterRow> rows) {
        List<BigDecimal> scores = rows.stream()
                .map(SurveyResponseFilterRow::getOverallScore)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (scores.isEmpty()) {
            return null;
        }
        BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    private List<SurveySatisfactionReportResponse.QuestionStat> buildQuestionStats(
            Map<String, SurveyQuestionEntity> questionMap,
            List<SurveyAnswerEntity> answers) {
        if (questionMap.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, List<SurveyAnswerEntity>> answersByQuestion = answers.stream()
                .filter(a -> a.getSurveyQuestionId() != null)
                .collect(Collectors.groupingBy(SurveyAnswerEntity::getSurveyQuestionId));

        List<SurveySatisfactionReportResponse.QuestionStat> stats = new ArrayList<>();
        for (Map.Entry<String, SurveyQuestionEntity> e : questionMap.entrySet()) {
            String questionId = e.getKey();
            SurveyQuestionEntity q = e.getValue();
            List<SurveyAnswerEntity> questionAnswers = answersByQuestion.getOrDefault(questionId, Collections.emptyList());
            SurveySatisfactionReportResponse.QuestionStat stat = new SurveySatisfactionReportResponse.QuestionStat();
            stat.setQuestionId(questionId);
            stat.setQuestionText(q.getContent());
            stat.setQuestionType(q.getType());
            stat.setResponseCount(questionAnswers.size());

            String type = q.getType() != null ? q.getType().toUpperCase() : "";
            if ("RATING".equals(type)) {
                List<Integer> ratings = questionAnswers.stream()
                        .map(SurveyAnswerEntity::getValueRating)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                if (!ratings.isEmpty()) {
                    double avg = ratings.stream().mapToInt(Integer::intValue).average().orElse(0);
                    stat.setAverageScore(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
                }
            } else if ("CHOICE".equals(type) || "MULTIPLE_CHOICE".equals(type)) {
                Map<String, Long> dist = questionAnswers.stream()
                        .map(SurveyAnswerEntity::getValueChoice)
                        .filter(Objects::nonNull)
                        .collect(Collectors.groupingBy(c -> c, Collectors.counting()));
                stat.setChoiceDistribution(dist);
            }
            stats.add(stat);
        }
        stats.sort(Comparator.comparing(s -> {
            SurveyQuestionEntity q = questionMap.get(s.getQuestionId());
            return (q != null && q.getSortOrder() != null) ? q.getSortOrder() : 0;
        }));
        return stats;
    }

    private static void validate(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
    }
}
