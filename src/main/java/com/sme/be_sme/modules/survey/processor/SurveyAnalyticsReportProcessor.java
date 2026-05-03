package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyAnalyticsReportRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyAnalyticsReportResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyAnswerMapperExt;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyInstanceMapperExt;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SurveyAnalyticsReportProcessor extends BaseBizProcessor<BizContext> {

    private static final BigDecimal SCORE_POSITIVE = BigDecimal.valueOf(4.2);
    private static final BigDecimal SCORE_STABLE = BigDecimal.valueOf(3.5);
    private static final BigDecimal SCORE_FOLLOW_UP = BigDecimal.valueOf(2.8);
    private static final BigDecimal TREND_THRESHOLD = BigDecimal.valueOf(0.5);
    private static final BigDecimal UNSTABLE_SPREAD_THRESHOLD = BigDecimal.valueOf(1.5);

    private final ObjectMapper objectMapper;
    private final SurveyResponseMapperExt surveyResponseMapperExt;
    private final SurveyAnswerMapperExt surveyAnswerMapperExt;
    private final SurveyQuestionMapper surveyQuestionMapper;
    private final SurveyInstanceMapperExt surveyInstanceMapperExt;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        validate(context);

        SurveyAnalyticsReportRequest req = payload != null && !payload.isNull()
                ? objectMapper.convertValue(payload, SurveyAnalyticsReportRequest.class)
                : new SurveyAnalyticsReportRequest();

        String companyId = context.getTenantId();

        List<SurveyResponseFilterRow> rows = surveyResponseMapperExt.selectByCompanyIdAndFilters(
                companyId,
                req.getStartDate(),
                req.getEndDate(),
                req.getTemplateId(),
                req.getStage() != null ? String.valueOf(req.getStage()) : null
        );

        int sentCount = surveyInstanceMapperExt.countSent(
                companyId,
                req.getTemplateId(),
                req.getStartDate(),
                req.getEndDate()
        );

        if (rows == null || rows.isEmpty()) {
            return buildEmptyResponse(sentCount);
        }

        List<String> responseIds = rows.stream()
                .map(SurveyResponseFilterRow::getSurveyResponseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        int submittedCount = responseIds.size();

        BigDecimal overall = avg(rows.stream()
                .map(SurveyResponseFilterRow::getOverallScore)
                .filter(Objects::nonNull)
                .toList()
        );

        List<SurveyAnswerEntity> answers = surveyAnswerMapperExt
                .selectByCompanyIdAndResponseIds(companyId, responseIds);

        if (answers == null) {
            answers = Collections.emptyList();
        }

        Set<String> templateIds = rows.stream()
                .map(SurveyResponseFilterRow::getSurveyTemplateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, SurveyQuestionEntity> questionMap = buildQuestionMap(templateIds);

        List<SurveyAnalyticsReportResponse.QuestionStat> questionStats =
                buildQuestionStats(questionMap, answers, submittedCount);

        List<SurveyAnalyticsReportResponse.DimensionStat> dimensionStats =
                buildDimensionStats(questionMap, answers);

        List<SurveyAnalyticsReportResponse.QuestionStat> ratingQuestions = questionStats.stream()
                .filter(q -> q.getAverageScore() != null)
                .toList();

        List<SurveyAnalyticsReportResponse.QuestionStat> lowestQuestions = ratingQuestions.stream()
                .sorted(Comparator.comparing(SurveyAnalyticsReportResponse.QuestionStat::getAverageScore))
                .limit(3)
                .toList();

        List<SurveyAnalyticsReportResponse.QuestionStat> highestQuestions = ratingQuestions.stream()
                .sorted(Comparator.comparing(
                        SurveyAnalyticsReportResponse.QuestionStat::getAverageScore,
                        Comparator.reverseOrder()
                ))
                .limit(3)
                .toList();

        List<SurveyAnalyticsReportResponse.DimensionStat> lowScoreDimensions = dimensionStats.stream()
                .filter(d -> d.getAverageScore() != null)
                .sorted(Comparator.comparing(SurveyAnalyticsReportResponse.DimensionStat::getAverageScore))
                .limit(3)
                .toList();

        List<SurveyAnalyticsReportResponse.DimensionStat> topPositiveDimensions = dimensionStats.stream()
                .filter(d -> d.getAverageScore() != null)
                .sorted(Comparator.comparing(
                        SurveyAnalyticsReportResponse.DimensionStat::getAverageScore,
                        Comparator.reverseOrder()
                ))
                .limit(3)
                .toList();

        List<SurveyAnalyticsReportResponse.TrendPoint> timeTrends = buildTimeTrends(rows);
        List<SurveyAnalyticsReportResponse.StageTrend> stageTrends = buildStageTrends(rows);

        int ratingQuestionCount = (int) questionMap.values().stream()
                .filter(q -> q != null && "RATING".equalsIgnoreCase(q.getType()))
                .count();

        int textQuestionCount = (int) questionMap.values().stream()
                .filter(q -> q != null && "TEXT".equalsIgnoreCase(q.getType()))
                .count();

        int choiceQuestionCount = (int) questionMap.values().stream()
                .filter(q -> q != null && isChoiceQuestion(q.getType()))
                .count();

        int textResponseCount = (int) answers.stream()
                .map(SurveyAnswerEntity::getValueText)
                .filter(v -> v != null && !v.isBlank())
                .count();

        Map<String, List<SurveyAnswerEntity>> answersByResponseId = answers.stream()
                .filter(a -> a.getSurveyResponseId() != null)
                .collect(Collectors.groupingBy(SurveyAnswerEntity::getSurveyResponseId));

        List<SurveyAnalyticsReportResponse.ResponseSummary> responseSummaries =
                buildResponseSummaries(rows, answersByResponseId, questionMap);

        List<SurveyAnalyticsReportResponse.EmployeeInsight> employeeInsights =
                buildEmployeeInsights(responseSummaries);

        SurveyAnalyticsReportResponse res = new SurveyAnalyticsReportResponse();
        res.setSentCount(sentCount);
        res.setSubmittedCount(submittedCount);
        res.setResponseRate(calcRate(sentCount, submittedCount));
        res.setOverallSatisfactionScore(overall);

        res.setQuestionStats(questionStats);
        res.setDimensionStats(dimensionStats);

        res.setLowestQuestions(lowestQuestions);
        res.setHighestQuestions(highestQuestions);
        res.setLowScoreDimensions(lowScoreDimensions);
        res.setTopPositiveDimensions(topPositiveDimensions);

        res.setTimeTrends(timeTrends);
        res.setStageTrends(stageTrends);

        res.setTextResponseCount(textResponseCount);
        res.setRatingQuestionCount(ratingQuestionCount);
        res.setTextQuestionCount(textQuestionCount);
        res.setChoiceQuestionCount(choiceQuestionCount);

        res.setResponseSummaries(responseSummaries);
        res.setEmployeeInsights(employeeInsights);

        return res;
    }

    private SurveyAnalyticsReportResponse buildEmptyResponse(Integer sentCount) {
        SurveyAnalyticsReportResponse empty = new SurveyAnalyticsReportResponse();

        empty.setSentCount(sentCount);
        empty.setSubmittedCount(0);
        empty.setResponseRate(calcRate(sentCount, 0));
        empty.setOverallSatisfactionScore(null);

        empty.setDimensionStats(Collections.emptyList());
        empty.setQuestionStats(Collections.emptyList());
        empty.setLowestQuestions(Collections.emptyList());
        empty.setHighestQuestions(Collections.emptyList());
        empty.setLowScoreDimensions(Collections.emptyList());
        empty.setTopPositiveDimensions(Collections.emptyList());
        empty.setTimeTrends(Collections.emptyList());
        empty.setStageTrends(Collections.emptyList());

        empty.setTextResponseCount(0);
        empty.setRatingQuestionCount(0);
        empty.setTextQuestionCount(0);
        empty.setChoiceQuestionCount(0);

        empty.setResponseSummaries(Collections.emptyList());
        empty.setEmployeeInsights(Collections.emptyList());

        return empty;
    }

    private static void validate(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
    }

    private Map<String, SurveyQuestionEntity> buildQuestionMap(Set<String> templateIds) {
        Map<String, SurveyQuestionEntity> questionMap = new HashMap<>();

        if (templateIds == null || templateIds.isEmpty()) {
            return questionMap;
        }

        for (String templateId : templateIds) {
            List<SurveyQuestionEntity> questions = surveyQuestionMapper.selectByTemplateId(templateId);
            if (questions == null) {
                continue;
            }

            for (SurveyQuestionEntity question : questions) {
                if (question != null && question.getSurveyQuestionId() != null) {
                    questionMap.putIfAbsent(question.getSurveyQuestionId(), question);
                }
            }
        }

        return questionMap;
    }

    private List<SurveyAnalyticsReportResponse.ResponseSummary> buildResponseSummaries(
            List<SurveyResponseFilterRow> rows,
            Map<String, List<SurveyAnswerEntity>> answersByResponseId,
            Map<String, SurveyQuestionEntity> questionMap
    ) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        return rows.stream()
                .map(row -> {
                    List<SurveyAnswerEntity> responseAnswers = answersByResponseId.getOrDefault(
                            row.getSurveyResponseId(),
                            Collections.emptyList()
                    );

                    SurveyAnalyticsReportResponse.ResponseSummary item =
                            new SurveyAnalyticsReportResponse.ResponseSummary();

                    item.setSurveyResponseId(row.getSurveyResponseId());
                    item.setSurveyInstanceId(row.getSurveyInstanceId());
                    item.setSurveyTemplateId(row.getSurveyTemplateId());
                    item.setTemplateName(row.getTemplateName());

                    item.setOnboardingId(row.getOnboardingId());
                    item.setStage(row.getStage());

                    item.setEmployeeId(row.getEmployeeId());
                    item.setEmployeeUserId(row.getEmployeeUserId());
                    item.setEmployeeName(row.getEmployeeName());
                    item.setEmployeeEmail(row.getEmployeeEmail());
                    item.setJobTitle(row.getJobTitle());
                    item.setDepartmentName(row.getDepartmentName());

                    item.setManagerUserId(row.getManagerUserId());
                    item.setManagerName(row.getManagerName());

                    item.setOverallScore(row.getOverallScore());
                    item.setSubmittedAt(row.getSubmittedAt());

                    item.setAnswerDetails(buildAnswerDetails(row, responseAnswers, questionMap));
                    item.setDimensionScores(buildDimensionScores(responseAnswers, questionMap));
                    item.setTextFeedbacks(buildTextFeedbacks(row, responseAnswers, questionMap));

                    return item;
                })
                .toList();
    }

    private List<SurveyAnalyticsReportResponse.EmployeeInsight> buildEmployeeInsights(
            List<SurveyAnalyticsReportResponse.ResponseSummary> responseSummaries
    ) {
        if (responseSummaries == null || responseSummaries.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<SurveyAnalyticsReportResponse.ResponseSummary>> byEmployee =
                responseSummaries.stream()
                        .collect(Collectors.groupingBy(this::employeeGroupKey));

        List<SurveyAnalyticsReportResponse.EmployeeInsight> out = new ArrayList<>();

        for (Map.Entry<String, List<SurveyAnalyticsReportResponse.ResponseSummary>> entry : byEmployee.entrySet()) {
            List<SurveyAnalyticsReportResponse.ResponseSummary> responses = entry.getValue();

            responses.sort(Comparator.comparing(
                    SurveyAnalyticsReportResponse.ResponseSummary::getSubmittedAt,
                    Comparator.nullsLast(Date::compareTo)
            ).reversed());

            SurveyAnalyticsReportResponse.ResponseSummary first = responses.get(0);

            List<SurveyAnalyticsReportResponse.ResponseSummary> scoredResponses = responses.stream()
                    .filter(r -> r.getOverallScore() != null)
                    .sorted(Comparator.comparing(
                            SurveyAnalyticsReportResponse.ResponseSummary::getSubmittedAt,
                            Comparator.nullsLast(Date::compareTo)
                    ))
                    .toList();

            List<BigDecimal> scores = scoredResponses.stream()
                    .map(SurveyAnalyticsReportResponse.ResponseSummary::getOverallScore)
                    .filter(Objects::nonNull)
                    .toList();

            BigDecimal averageScore = avg(scores);
            BigDecimal latestScore = scores.isEmpty() ? null : scores.get(scores.size() - 1);
            BigDecimal lowestScore = scores.stream().min(BigDecimal::compareTo).orElse(null);
            BigDecimal highestScore = scores.stream().max(BigDecimal::compareTo).orElse(null);
            BigDecimal scoreSpread = highestScore != null && lowestScore != null
                    ? highestScore.subtract(lowestScore).abs().setScale(2, RoundingMode.HALF_UP)
                    : null;

            SurveyAnalyticsReportResponse.ResponseSummary latestScoredResponse =
                    scoredResponses.isEmpty() ? null : scoredResponses.get(scoredResponses.size() - 1);

            SurveyAnalyticsReportResponse.ResponseSummary lowestResponse = scoredResponses.stream()
                    .min(Comparator.comparing(SurveyAnalyticsReportResponse.ResponseSummary::getOverallScore))
                    .orElse(null);

            SurveyAnalyticsReportResponse.ResponseSummary highestResponse = scoredResponses.stream()
                    .max(Comparator.comparing(SurveyAnalyticsReportResponse.ResponseSummary::getOverallScore))
                    .orElse(null);

            String trend = calcTrend(scoredResponses);
            String riskLevel = calcRiskLevel(averageScore, scores.size());

            List<SurveyAnalyticsReportResponse.DimensionScore> dimensionScores =
                    mergeDimensionScores(responses);

            List<SurveyAnalyticsReportResponse.DimensionScore> weakDimensions = dimensionScores.stream()
                    .filter(d -> d.getAverageScore() != null)
                    .sorted(Comparator.comparing(SurveyAnalyticsReportResponse.DimensionScore::getAverageScore))
                    .limit(3)
                    .toList();

            List<SurveyAnalyticsReportResponse.DimensionScore> strongDimensions = dimensionScores.stream()
                    .filter(d -> d.getAverageScore() != null)
                    .sorted(Comparator.comparing(
                            SurveyAnalyticsReportResponse.DimensionScore::getAverageScore,
                            Comparator.reverseOrder()
                    ))
                    .limit(3)
                    .toList();

            List<SurveyAnalyticsReportResponse.TextFeedback> textFeedbacks = responses.stream()
                    .flatMap(r -> safeList(r.getTextFeedbacks()).stream())
                    .limit(10)
                    .toList();

            SurveyAnalyticsReportResponse.EmployeeInsight insight =
                    new SurveyAnalyticsReportResponse.EmployeeInsight();

            insight.setEmployeeKey(entry.getKey());

            insight.setEmployeeId(first.getEmployeeId());
            insight.setEmployeeUserId(first.getEmployeeUserId());
            insight.setEmployeeName(first.getEmployeeName());
            insight.setEmployeeEmail(first.getEmployeeEmail());
            insight.setJobTitle(first.getJobTitle());
            insight.setDepartmentName(first.getDepartmentName());

            insight.setManagerUserId(first.getManagerUserId());
            insight.setManagerName(first.getManagerName());

            insight.setResponseCount(responses.size());
            insight.setValidScoreCount(scores.size());

            insight.setAverageScore(averageScore);
            insight.setLatestScore(latestScore);
            insight.setHighestScore(highestScore);
            insight.setLowestScore(lowestScore);
            insight.setScoreSpread(scoreSpread);

            insight.setLatestTemplateName(latestScoredResponse != null ? latestScoredResponse.getTemplateName() : null);
            insight.setLowestTemplateName(lowestResponse != null ? lowestResponse.getTemplateName() : null);
            insight.setHighestTemplateName(highestResponse != null ? highestResponse.getTemplateName() : null);

            insight.setLatestSubmittedAt(first.getSubmittedAt());

            insight.setTrend(trend);
            insight.setRiskLevel(riskLevel);

            insight.setDimensionScores(dimensionScores);
            insight.setWeakDimensions(weakDimensions);
            insight.setStrongDimensions(strongDimensions);

            insight.setTextFeedbacks(textFeedbacks);
            insight.setResponses(responses);

            insight.setSummary(buildEmployeeSummary(insight));
            insight.setRecommendation(buildEmployeeRecommendation(insight));

            out.add(insight);
        }

        out.sort((a, b) -> {
            int riskCompare = Integer.compare(riskOrder(a.getRiskLevel()), riskOrder(b.getRiskLevel()));
            if (riskCompare != 0) {
                return riskCompare;
            }

            BigDecimal aScore = a.getAverageScore() == null ? BigDecimal.valueOf(999) : a.getAverageScore();
            BigDecimal bScore = b.getAverageScore() == null ? BigDecimal.valueOf(999) : b.getAverageScore();

            return aScore.compareTo(bScore);
        });

        return out;
    }

    private List<SurveyAnalyticsReportResponse.AnswerDetail> buildAnswerDetails(
            SurveyResponseFilterRow row,
            List<SurveyAnswerEntity> answers,
            Map<String, SurveyQuestionEntity> questionMap
    ) {
        if (answers == null || answers.isEmpty()) {
            return Collections.emptyList();
        }

        List<SurveyAnalyticsReportResponse.AnswerDetail> out = new ArrayList<>();

        for (SurveyAnswerEntity answer : answers) {
            SurveyQuestionEntity question = questionMap.get(answer.getSurveyQuestionId());

            SurveyAnalyticsReportResponse.AnswerDetail detail =
                    new SurveyAnalyticsReportResponse.AnswerDetail();

            detail.setSurveyResponseId(row.getSurveyResponseId());
            detail.setSurveyInstanceId(row.getSurveyInstanceId());

            detail.setQuestionId(answer.getSurveyQuestionId());
            detail.setQuestionText(question != null ? question.getContent() : null);
            detail.setQuestionType(question != null ? question.getType() : answer.getQuestionType());
            detail.setDimensionCode(question != null ? question.getDimensionCode() : answer.getDimensionCode());

            detail.setValueRating(answer.getValueRating());
            detail.setValueText(answer.getValueText());
            detail.setValueChoice(answer.getValueChoice());

            String type = detail.getQuestionType() == null ? "" : detail.getQuestionType().trim().toUpperCase();
            if ("MULTIPLE_CHOICE".equals(type)) {
                detail.setValueChoices(parseMultiChoiceValues(answer.getValueText()).toList());
            } else {
                detail.setValueChoices(Collections.emptyList());
            }

            out.add(detail);
        }

        return out;
    }

    private List<SurveyAnalyticsReportResponse.TextFeedback> buildTextFeedbacks(
            SurveyResponseFilterRow row,
            List<SurveyAnswerEntity> answers,
            Map<String, SurveyQuestionEntity> questionMap
    ) {
        if (answers == null || answers.isEmpty()) {
            return Collections.emptyList();
        }

        List<SurveyAnalyticsReportResponse.TextFeedback> out = new ArrayList<>();

        for (SurveyAnswerEntity answer : answers) {
            SurveyQuestionEntity question = questionMap.get(answer.getSurveyQuestionId());

            String type = question != null ? question.getType() : answer.getQuestionType();
            if (!"TEXT".equalsIgnoreCase(type)) {
                continue;
            }

            String value = answer.getValueText();
            if (value == null || value.isBlank()) {
                continue;
            }

            SurveyAnalyticsReportResponse.TextFeedback feedback =
                    new SurveyAnalyticsReportResponse.TextFeedback();

            feedback.setSurveyResponseId(row.getSurveyResponseId());
            feedback.setSurveyInstanceId(row.getSurveyInstanceId());
            feedback.setTemplateName(row.getTemplateName());
            feedback.setQuestionId(answer.getSurveyQuestionId());
            feedback.setQuestionText(question != null ? question.getContent() : null);
            feedback.setDimensionCode(question != null ? question.getDimensionCode() : answer.getDimensionCode());
            feedback.setAnswer(value.trim());
            feedback.setSubmittedAt(row.getSubmittedAt());

            out.add(feedback);
        }

        return out;
    }

    private List<SurveyAnalyticsReportResponse.DimensionScore> buildDimensionScores(
            List<SurveyAnswerEntity> answers,
            Map<String, SurveyQuestionEntity> questionMap
    ) {
        if (answers == null || answers.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<Integer>> ratingsByDimension = new HashMap<>();

        for (SurveyAnswerEntity answer : answers) {
            SurveyQuestionEntity question = questionMap.get(answer.getSurveyQuestionId());

            String type = question != null ? question.getType() : answer.getQuestionType();
            if (!"RATING".equalsIgnoreCase(type)) {
                continue;
            }

            if (question != null && question.getMeasurable() != null && !question.getMeasurable()) {
                continue;
            }

            String dimension = question != null ? question.getDimensionCode() : answer.getDimensionCode();
            if (dimension == null || dimension.isBlank()) {
                continue;
            }

            Integer rating = answer.getValueRating();
            if (rating == null) {
                continue;
            }

            ratingsByDimension
                    .computeIfAbsent(dimension.trim(), key -> new ArrayList<>())
                    .add(rating);
        }

        List<SurveyAnalyticsReportResponse.DimensionScore> out = new ArrayList<>();

        for (Map.Entry<String, List<Integer>> entry : ratingsByDimension.entrySet()) {
            BigDecimal average = avg(entry.getValue().stream()
                    .map(BigDecimal::valueOf)
                    .toList());

            SurveyAnalyticsReportResponse.DimensionScore score =
                    new SurveyAnalyticsReportResponse.DimensionScore();

            score.setDimensionCode(entry.getKey());
            score.setAverageScore(average);
            score.setAnswerCount(entry.getValue().size());
            score.setLevel(calcDimensionLevel(average));

            out.add(score);
        }

        out.sort(Comparator.comparing(
                SurveyAnalyticsReportResponse.DimensionScore::getAverageScore,
                Comparator.nullsLast(BigDecimal::compareTo)
        ));

        return out;
    }

    private List<SurveyAnalyticsReportResponse.DimensionScore> mergeDimensionScores(
            List<SurveyAnalyticsReportResponse.ResponseSummary> responses
    ) {
        Map<String, List<BigDecimal>> scoresByDimension = new HashMap<>();
        Map<String, Integer> countByDimension = new HashMap<>();

        for (SurveyAnalyticsReportResponse.ResponseSummary response : safeList(responses)) {
            for (SurveyAnalyticsReportResponse.DimensionScore dimension : safeList(response.getDimensionScores())) {
                if (dimension.getDimensionCode() == null || dimension.getAverageScore() == null) {
                    continue;
                }

                scoresByDimension
                        .computeIfAbsent(dimension.getDimensionCode(), key -> new ArrayList<>())
                        .add(dimension.getAverageScore());

                countByDimension.put(
                        dimension.getDimensionCode(),
                        countByDimension.getOrDefault(dimension.getDimensionCode(), 0)
                                + Optional.ofNullable(dimension.getAnswerCount()).orElse(0)
                );
            }
        }

        List<SurveyAnalyticsReportResponse.DimensionScore> out = new ArrayList<>();

        for (Map.Entry<String, List<BigDecimal>> entry : scoresByDimension.entrySet()) {
            BigDecimal average = avg(entry.getValue());

            SurveyAnalyticsReportResponse.DimensionScore score =
                    new SurveyAnalyticsReportResponse.DimensionScore();

            score.setDimensionCode(entry.getKey());
            score.setAverageScore(average);
            score.setAnswerCount(countByDimension.getOrDefault(entry.getKey(), 0));
            score.setLevel(calcDimensionLevel(average));

            out.add(score);
        }

        out.sort(Comparator.comparing(
                SurveyAnalyticsReportResponse.DimensionScore::getAverageScore,
                Comparator.nullsLast(BigDecimal::compareTo)
        ));

        return out;
    }

    private List<SurveyAnalyticsReportResponse.QuestionStat> buildQuestionStats(
            Map<String, SurveyQuestionEntity> questionMap,
            List<SurveyAnswerEntity> answers,
            int submittedCount
    ) {
        if (questionMap == null || questionMap.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<SurveyAnswerEntity>> byQuestion = answers.stream()
                .filter(a -> a.getSurveyQuestionId() != null)
                .collect(Collectors.groupingBy(SurveyAnswerEntity::getSurveyQuestionId));

        List<SurveyAnalyticsReportResponse.QuestionStat> out = new ArrayList<>();

        for (SurveyQuestionEntity q : questionMap.values()) {
            if (q == null || q.getSurveyQuestionId() == null) {
                continue;
            }

            List<SurveyAnswerEntity> ans = byQuestion.getOrDefault(
                    q.getSurveyQuestionId(),
                    Collections.emptyList()
            );

            SurveyAnalyticsReportResponse.QuestionStat stat =
                    new SurveyAnalyticsReportResponse.QuestionStat();

            stat.setQuestionId(q.getSurveyQuestionId());
            stat.setQuestionText(q.getContent());
            stat.setQuestionType(q.getType());
            stat.setDimensionCode(q.getDimensionCode());
            stat.setResponseCount(ans.size());

            if (submittedCount > 0) {
                BigDecimal completionRate = BigDecimal.valueOf(ans.size())
                        .divide(BigDecimal.valueOf(submittedCount), 4, RoundingMode.HALF_UP);
                stat.setCompletionRate(completionRate);
            }

            String type = q.getType() == null ? "" : q.getType().trim().toUpperCase();

            if ("RATING".equals(type)) {
                List<Integer> ratings = ans.stream()
                        .map(SurveyAnswerEntity::getValueRating)
                        .filter(Objects::nonNull)
                        .toList();

                if (!ratings.isEmpty()) {
                    double average = ratings.stream()
                            .mapToInt(Integer::intValue)
                            .average()
                            .orElse(0);

                    stat.setAverageScore(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
                }

            } else if ("CHOICE".equals(type) || "SINGLE_CHOICE".equals(type)) {
                Map<String, Long> distribution = ans.stream()
                        .map(SurveyAnswerEntity::getValueChoice)
                        .filter(v -> v != null && !v.isBlank())
                        .map(String::trim)
                        .collect(Collectors.groupingBy(value -> value, Collectors.counting()));

                stat.setChoiceDistribution(distribution.isEmpty() ? null : distribution);

            } else if ("MULTIPLE_CHOICE".equals(type)) {
                Map<String, Long> distribution = ans.stream()
                        .map(SurveyAnswerEntity::getValueText)
                        .filter(v -> v != null && !v.isBlank())
                        .flatMap(this::parseMultiChoiceValues)
                        .collect(Collectors.groupingBy(value -> value, Collectors.counting()));

                stat.setChoiceDistribution(distribution.isEmpty() ? null : distribution);

            } else if ("TEXT".equals(type)) {
                List<String> texts = ans.stream()
                        .map(SurveyAnswerEntity::getValueText)
                        .filter(v -> v != null && !v.isBlank())
                        .map(String::trim)
                        .toList();

                stat.setTextAnswerCount(texts.size());
                stat.setSampleTexts(texts.isEmpty() ? null : texts.stream().limit(3).toList());
            }

            out.add(stat);
        }

        out.sort(Comparator.comparingInt(stat -> {
            SurveyQuestionEntity question = questionMap.get(stat.getQuestionId());
            return question != null && question.getSortOrder() != null
                    ? question.getSortOrder()
                    : 0;
        }));

        return out;
    }

    private List<SurveyAnalyticsReportResponse.DimensionStat> buildDimensionStats(
            Map<String, SurveyQuestionEntity> questionMap,
            List<SurveyAnswerEntity> answers
    ) {
        if (questionMap == null || questionMap.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<SurveyAnswerEntity>> byQuestion = answers.stream()
                .filter(a -> a.getSurveyQuestionId() != null)
                .collect(Collectors.groupingBy(SurveyAnswerEntity::getSurveyQuestionId));

        Map<String, List<Integer>> ratingsByDimension = new HashMap<>();
        Map<String, Integer> questionCountByDimension = new HashMap<>();
        Map<String, Integer> answerCountByDimension = new HashMap<>();

        for (SurveyQuestionEntity q : questionMap.values()) {
            if (q == null) {
                continue;
            }

            if (q.getMeasurable() != null && !q.getMeasurable()) {
                continue;
            }

            if (q.getDimensionCode() == null || q.getDimensionCode().isBlank()) {
                continue;
            }

            if (q.getType() == null || !"RATING".equalsIgnoreCase(q.getType())) {
                continue;
            }

            String dimension = q.getDimensionCode().trim();

            questionCountByDimension.put(
                    dimension,
                    questionCountByDimension.getOrDefault(dimension, 0) + 1
            );

            List<SurveyAnswerEntity> ans = byQuestion.getOrDefault(
                    q.getSurveyQuestionId(),
                    Collections.emptyList()
            );

            answerCountByDimension.put(
                    dimension,
                    answerCountByDimension.getOrDefault(dimension, 0) + ans.size()
            );

            List<Integer> ratings = ans.stream()
                    .map(SurveyAnswerEntity::getValueRating)
                    .filter(Objects::nonNull)
                    .toList();

            if (!ratings.isEmpty()) {
                ratingsByDimension
                        .computeIfAbsent(dimension, key -> new ArrayList<>())
                        .addAll(ratings);
            }
        }

        List<SurveyAnalyticsReportResponse.DimensionStat> out = new ArrayList<>();

        for (String dimension : questionCountByDimension.keySet()) {
            SurveyAnalyticsReportResponse.DimensionStat stat =
                    new SurveyAnalyticsReportResponse.DimensionStat();

            stat.setDimensionCode(dimension);
            stat.setQuestionCount(questionCountByDimension.get(dimension));
            stat.setResponseCount(answerCountByDimension.getOrDefault(dimension, 0));

            List<Integer> ratings = ratingsByDimension.getOrDefault(dimension, Collections.emptyList());
            if (!ratings.isEmpty()) {
                double average = ratings.stream()
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0);

                stat.setAverageScore(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
            }

            out.add(stat);
        }

        out.sort(Comparator.comparing(
                SurveyAnalyticsReportResponse.DimensionStat::getAverageScore,
                Comparator.nullsLast(BigDecimal::compareTo)
        ));

        return out;
    }

    private List<SurveyAnalyticsReportResponse.TrendPoint> buildTimeTrends(
            List<SurveyResponseFilterRow> rows
    ) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM");

        Map<String, List<SurveyResponseFilterRow>> byBucket = rows.stream()
                .filter(row -> extractTrendDate(row) != null)
                .collect(Collectors.groupingBy(row -> monthFormat.format(extractTrendDate(row))));

        List<SurveyAnalyticsReportResponse.TrendPoint> out = new ArrayList<>();

        for (Map.Entry<String, List<SurveyResponseFilterRow>> entry : byBucket.entrySet()) {
            List<SurveyResponseFilterRow> bucketRows = entry.getValue();

            SurveyAnalyticsReportResponse.TrendPoint point =
                    new SurveyAnalyticsReportResponse.TrendPoint();

            point.setBucket(entry.getKey());
            point.setSubmittedCount((int) bucketRows.stream()
                    .map(SurveyResponseFilterRow::getSurveyResponseId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count());

            point.setAverageScore(avg(bucketRows.stream()
                    .map(SurveyResponseFilterRow::getOverallScore)
                    .filter(Objects::nonNull)
                    .toList()));

            out.add(point);
        }

        out.sort(Comparator.comparing(SurveyAnalyticsReportResponse.TrendPoint::getBucket));
        return out;
    }

    private List<SurveyAnalyticsReportResponse.StageTrend> buildStageTrends(
            List<SurveyResponseFilterRow> rows
    ) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<SurveyResponseFilterRow>> byStage = rows.stream()
                .filter(r -> r.getStage() != null && !r.getStage().isBlank())
                .collect(Collectors.groupingBy(r -> r.getStage().trim()));

        List<SurveyAnalyticsReportResponse.StageTrend> out = new ArrayList<>();

        for (Map.Entry<String, List<SurveyResponseFilterRow>> entry : byStage.entrySet()) {
            List<SurveyResponseFilterRow> stageRows = entry.getValue();

            SurveyAnalyticsReportResponse.StageTrend trend =
                    new SurveyAnalyticsReportResponse.StageTrend();

            trend.setStage(entry.getKey());
            trend.setSubmittedCount((int) stageRows.stream()
                    .map(SurveyResponseFilterRow::getSurveyResponseId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count());

            trend.setAverageOverall(avg(stageRows.stream()
                    .map(SurveyResponseFilterRow::getOverallScore)
                    .filter(Objects::nonNull)
                    .toList()));

            out.add(trend);
        }

        out.sort(Comparator.comparing(t -> normalizeStageOrder(t.getStage())));
        return out;
    }

    private Date extractTrendDate(SurveyResponseFilterRow row) {
        if (row == null) {
            return null;
        }

        return row.getSubmittedAt();
    }

    private String employeeGroupKey(SurveyAnalyticsReportResponse.ResponseSummary item) {
        if (item == null) {
            return "UNKNOWN";
        }

        if (StringUtils.hasText(item.getEmployeeUserId())) {
            return item.getEmployeeUserId();
        }

        if (StringUtils.hasText(item.getEmployeeId())) {
            return item.getEmployeeId();
        }

        if (StringUtils.hasText(item.getEmployeeName())) {
            return "NAME:" + item.getEmployeeName().trim().toLowerCase();
        }

        return "UNKNOWN:" + Optional.ofNullable(item.getSurveyInstanceId()).orElse(UUID.randomUUID().toString());
    }

    private String calcTrend(List<SurveyAnalyticsReportResponse.ResponseSummary> chronologicalScoredResponses) {
        if (chronologicalScoredResponses == null || chronologicalScoredResponses.isEmpty()) {
            return "NO_SCORE";
        }

        if (chronologicalScoredResponses.size() == 1) {
            return "SINGLE";
        }

        BigDecimal first = chronologicalScoredResponses.get(0).getOverallScore();
        BigDecimal latest = chronologicalScoredResponses.get(chronologicalScoredResponses.size() - 1).getOverallScore();

        if (first == null || latest == null) {
            return "NO_SCORE";
        }

        BigDecimal diff = latest.subtract(first);

        if (diff.compareTo(TREND_THRESHOLD) >= 0) {
            return "IMPROVING";
        }

        if (diff.compareTo(TREND_THRESHOLD.negate()) <= 0) {
            return "DECLINING";
        }

        return "STABLE";
    }

    private String calcRiskLevel(BigDecimal averageScore, int validScoreCount) {
        if (validScoreCount <= 0 || averageScore == null) {
            return "NO_SCORE";
        }

        if (averageScore.compareTo(SCORE_POSITIVE) >= 0) {
            return "POSITIVE";
        }

        if (averageScore.compareTo(SCORE_STABLE) >= 0) {
            return "STABLE";
        }

        if (averageScore.compareTo(SCORE_FOLLOW_UP) >= 0) {
            return "NEED_FOLLOW_UP";
        }

        return "RISK";
    }

    private String calcDimensionLevel(BigDecimal averageScore) {
        if (averageScore == null) {
            return "NO_SCORE";
        }

        if (averageScore.compareTo(SCORE_POSITIVE) >= 0) {
            return "GOOD";
        }

        if (averageScore.compareTo(SCORE_STABLE) >= 0) {
            return "NORMAL";
        }

        if (averageScore.compareTo(SCORE_FOLLOW_UP) >= 0) {
            return "LOW";
        }

        return "RISK";
    }

    private String buildEmployeeSummary(SurveyAnalyticsReportResponse.EmployeeInsight insight) {
        if (insight == null || insight.getValidScoreCount() == null || insight.getValidScoreCount() <= 0) {
            return "Nhân viên đã có phản hồi khảo sát nhưng chưa có dữ liệu điểm để đánh giá mức độ hài lòng.";
        }

        String name = safeText(insight.getEmployeeName(), "Nhân viên");
        String weak = formatDimensionList(insight.getWeakDimensions());
        String trendText = trendText(insight.getTrend());

        if ("RISK".equals(insight.getRiskLevel())) {
            return String.format(
                    "%s có dấu hiệu rủi ro trong onboarding. Điểm trung bình %s/5, điểm thấp nhất %s/5 ở mẫu \"%s\". Nhóm cần chú ý: %s. Xu hướng hiện tại: %s.",
                    name,
                    formatScore(insight.getAverageScore()),
                    formatScore(insight.getLowestScore()),
                    safeText(insight.getLowestTemplateName(), "—"),
                    weak,
                    trendText
            );
        }

        if ("NEED_FOLLOW_UP".equals(insight.getRiskLevel())) {
            return String.format(
                    "%s cần được theo dõi thêm. Điểm trung bình %s/5, điểm mới nhất %s/5, điểm thấp nhất %s/5 ở mẫu \"%s\". Nhóm yếu nhất: %s. Xu hướng hiện tại: %s.",
                    name,
                    formatScore(insight.getAverageScore()),
                    formatScore(insight.getLatestScore()),
                    formatScore(insight.getLowestScore()),
                    safeText(insight.getLowestTemplateName(), "—"),
                    weak,
                    trendText
            );
        }

        if ("STABLE".equals(insight.getRiskLevel())) {
            return String.format(
                    "%s có trải nghiệm onboarding tương đối ổn định. Điểm trung bình %s/5, điểm mới nhất %s/5. Nhóm cần cải thiện thêm: %s. Xu hướng hiện tại: %s.",
                    name,
                    formatScore(insight.getAverageScore()),
                    formatScore(insight.getLatestScore()),
                    weak,
                    trendText
            );
        }

        return String.format(
                "%s có trải nghiệm onboarding tích cực. Điểm trung bình %s/5, điểm mới nhất %s/5, điểm cao nhất %s/5. Nhóm thể hiện tốt: %s. Xu hướng hiện tại: %s.",
                name,
                formatScore(insight.getAverageScore()),
                formatScore(insight.getLatestScore()),
                formatScore(insight.getHighestScore()),
                formatDimensionList(insight.getStrongDimensions()),
                trendText
        );
    }

    private String buildEmployeeRecommendation(SurveyAnalyticsReportResponse.EmployeeInsight insight) {
        if (insight == null || insight.getValidScoreCount() == null || insight.getValidScoreCount() <= 0) {
            return "HR nên bổ sung thêm câu hỏi rating trong template khảo sát để có đủ dữ liệu đánh giá định lượng.";
        }

        if ("DECLINING".equals(insight.getTrend())) {
            return String.format(
                    "Điểm khảo sát của nhân viên đang giảm. HR nên trao đổi 1:1 để tìm nguyên nhân mới phát sinh, đặc biệt sau lần khảo sát gần nhất có điểm %s/5.",
                    formatScore(insight.getLatestScore())
            );
        }

        if ("RISK".equals(insight.getRiskLevel())) {
            return "HR cần can thiệp sớm, đặt lịch trao đổi 1:1, kiểm tra lại onboarding plan, mức hỗ trợ từ manager, tài liệu training và khả năng hòa nhập đội nhóm.";
        }

        if ("NEED_FOLLOW_UP".equals(insight.getRiskLevel())) {
            return String.format(
                    "HR nên follow-up với nhân viên này, bắt đầu từ mẫu khảo sát có điểm thấp nhất \"%s\" và các nhóm điểm yếu: %s.",
                    safeText(insight.getLowestTemplateName(), "—"),
                    formatDimensionList(insight.getWeakDimensions())
            );
        }

        if ("POSITIVE".equals(insight.getRiskLevel())
                && insight.getScoreSpread() != null
                && insight.getScoreSpread().compareTo(UNSTABLE_SPREAD_THRESHOLD) >= 0) {
            return String.format(
                    "Nhân viên có điểm trung bình tốt nhưng điểm dao động khá lớn. HR nên xem lại mẫu khảo sát thấp nhất \"%s\" để hiểu phần trải nghiệm chưa ổn định.",
                    safeText(insight.getLowestTemplateName(), "—")
            );
        }

        if ("STABLE".equals(insight.getRiskLevel())) {
            return "HR nên tiếp tục theo dõi ở mốc khảo sát tiếp theo và hỏi nhanh nhân viên về tài liệu, training, hỗ trợ từ manager và mức độ hòa nhập team.";
        }

        return "Nhân viên đang thích nghi tốt. HR có thể duy trì cách onboarding hiện tại và dùng trường hợp này làm ví dụ tốt cho các đợt onboarding sau.";
    }

    private String formatDimensionList(List<SurveyAnalyticsReportResponse.DimensionScore> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return "chưa đủ dữ liệu theo nhóm";
        }

        return dimensions.stream()
                .limit(3)
                .map(d -> d.getDimensionCode() + " (" + formatScore(d.getAverageScore()) + "/5)")
                .collect(Collectors.joining(", "));
    }

    private String trendText(String trend) {
        if ("IMPROVING".equals(trend)) {
            return "đang cải thiện";
        }

        if ("DECLINING".equals(trend)) {
            return "có dấu hiệu giảm";
        }

        if ("STABLE".equals(trend)) {
            return "ổn định";
        }

        if ("SINGLE".equals(trend)) {
            return "chỉ có một phản hồi";
        }

        return "chưa đủ dữ liệu xu hướng";
    }

    private String formatScore(BigDecimal score) {
        if (score == null) {
            return "—";
        }

        return score.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String safeText(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private static BigDecimal calcRate(Integer sent, Integer submitted) {
        if (submitted == null) {
            return BigDecimal.ZERO;
        }

        if (sent == null || sent <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(submitted)
                .divide(BigDecimal.valueOf(sent), 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal avg(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private boolean isChoiceQuestion(String type) {
        if (type == null) {
            return false;
        }

        return "CHOICE".equalsIgnoreCase(type)
                || "SINGLE_CHOICE".equalsIgnoreCase(type)
                || "MULTIPLE_CHOICE".equalsIgnoreCase(type);
    }

    private java.util.stream.Stream<String> parseMultiChoiceValues(String raw) {
        if (raw == null || raw.isBlank()) {
            return java.util.stream.Stream.empty();
        }

        String value = raw.trim();

        if (value.startsWith("[") && value.endsWith("]")) {
            try {
                String[] arr = objectMapper.readValue(value, String[].class);

                return Arrays.stream(arr)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isBlank());
            } catch (Exception ignored) {
                // fallback below
            }
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .map(s -> s.replace("\"", ""))
                .filter(s -> !s.isBlank());
    }

    private int normalizeStageOrder(String stage) {
        if (stage == null) {
            return 999;
        }

        String s = stage.trim().toUpperCase();

        if ("D7".equals(s) || "DAY_7".equals(s)) {
            return 7;
        }

        if ("D30".equals(s) || "DAY_30".equals(s)) {
            return 30;
        }

        if ("D60".equals(s) || "DAY_60".equals(s)) {
            return 60;
        }

        if ("CUSTOM".equals(s)) {
            return 999;
        }

        return 500;
    }

    private static <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
    private int riskOrder(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return 99;
        }

        String value = riskLevel.trim().toUpperCase();

        switch (value) {
            case "RISK":
                return 1;
            case "NEED_FOLLOW_UP":
                return 2;
            case "STABLE":
                return 3;
            case "POSITIVE":
                return 4;
            case "NO_SCORE":
                return 5;
            default:
                return 99;
        }
    }
}