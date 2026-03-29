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

        Integer sentCount = null;
        try {
            sentCount = surveyInstanceMapperExt.countSent(
                    companyId,
                    req.getTemplateId(),
                    req.getStartDate(),
                    req.getEndDate()
            );
        } catch (Exception ignored) {

        }

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

        Map<String, SurveyQuestionEntity> questionMap = new HashMap<>();
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

        int ratingQuestionCount = (int) questionMap.values().stream()
                .filter(q -> q != null && "RATING".equalsIgnoreCase(q.getType()))
                .count();

        int textQuestionCount = (int) questionMap.values().stream()
                .filter(q -> q != null && "TEXT".equalsIgnoreCase(q.getType()))
                .count();

        int choiceQuestionCount = (int) questionMap.values().stream()
                .filter(q -> q != null && (
                        "CHOICE".equalsIgnoreCase(q.getType()) ||
                                "MULTIPLE_CHOICE".equalsIgnoreCase(q.getType())
                ))
                .count();

        int textResponseCount = (int) answers.stream()
                .map(SurveyAnswerEntity::getValueText)
                .filter(v -> v != null && !v.isBlank())
                .count();

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

        res.setTextResponseCount(textResponseCount);
        res.setRatingQuestionCount(ratingQuestionCount);
        res.setTextQuestionCount(textQuestionCount);
        res.setChoiceQuestionCount(choiceQuestionCount);

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

        empty.setTextResponseCount(0);
        empty.setRatingQuestionCount(0);
        empty.setTextQuestionCount(0);
        empty.setChoiceQuestionCount(0);
        return empty;
    }

    private static void validate(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
    }

    private static BigDecimal calcRate(Integer sent, Integer submitted) {
        if (sent == null || sent <= 0 || submitted == null) {
            return null;
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

            }
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .map(s -> s.replace("\"", ""))
                .filter(s -> !s.isBlank());
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

    private Date extractTrendDate(SurveyResponseFilterRow row) {
        if (row == null) {
            return null;
        }
        try {
            return row.getSubmittedAt();
        } catch (Exception ignored) {
            return null;
        }
    }
}