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

        // 1) load response rows (overall_score + responseId + templateId )
        List<SurveyResponseFilterRow> rows = surveyResponseMapperExt.selectByCompanyIdAndFilters(
                companyId,
                req.getStartDate(),
                req.getEndDate(),
                req.getTemplateId(),
                req.getStage() != null ? String.valueOf(req.getStage()) : null

        );

        // sent count (optional)
        Integer sentCount = null;
        if (surveyInstanceMapperExt != null) {
            try {
                sentCount = surveyInstanceMapperExt.countSent(
                        companyId, req.getTemplateId(),  req.getStartDate(), req.getEndDate()
                );
            } catch (Exception ignored) {  }
        }

        if (rows == null || rows.isEmpty()) {
            SurveyAnalyticsReportResponse empty = new SurveyAnalyticsReportResponse();
            empty.setSentCount(sentCount);
            empty.setSubmittedCount(0);
            empty.setResponseRate(calcRate(sentCount, 0));
            empty.setOverallSatisfactionScore(null);
            empty.setDimensionStats(Collections.emptyList());
            empty.setQuestionStats(Collections.emptyList());
            empty.setStageTrends(Collections.emptyList());
            return empty;
        }

        List<String> responseIds = rows.stream()
                .map(SurveyResponseFilterRow::getSurveyResponseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        int submittedCount = responseIds.size();

        // 2) overall score
        BigDecimal overall = avg(rows.stream()
                .map(SurveyResponseFilterRow::getOverallScore)
                .filter(Objects::nonNull)
                .toList()
        );

        // 3) load all answers
        List<SurveyAnswerEntity> answers = surveyAnswerMapperExt
                .selectByCompanyIdAndResponseIds(companyId, responseIds);
        if (answers == null) answers = Collections.emptyList();

        // 4) load questions (from templates in result set)
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

        // 5) question stats
        List<SurveyAnalyticsReportResponse.QuestionStat> questionStats =
                buildQuestionStats(questionMap, answers);

        // 6) dimension stats (only measurable=true + rating)
        List<SurveyAnalyticsReportResponse.DimensionStat> dimensionStats =
                buildDimensionStats(questionMap, answers);

        // 7) stage trends (if stage info is available in SurveyResponseFilterRow)
//        List<SurveyAnalyticsReportResponse.StageTrend> stageTrends =
//                buildStageTrends(rows);

        // 8) response
        SurveyAnalyticsReportResponse res = new SurveyAnalyticsReportResponse();
        res.setSentCount(sentCount);
        res.setSubmittedCount(submittedCount);
        res.setResponseRate(calcRate(sentCount, submittedCount));
        res.setOverallSatisfactionScore(overall);
        res.setQuestionStats(questionStats);
        res.setDimensionStats(dimensionStats);
//        res.setStageTrends(stageTrends);
        return res;
    }

    private static void validate(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
    }

    private static BigDecimal calcRate(Integer sent, Integer submitted) {
        if (sent == null || sent <= 0 || submitted == null) return null;
        return BigDecimal.valueOf(submitted)
                .divide(BigDecimal.valueOf(sent), 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal avg(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) return null;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private List<SurveyAnalyticsReportResponse.QuestionStat> buildQuestionStats(
            Map<String, SurveyQuestionEntity> questionMap,
            List<SurveyAnswerEntity> answers
    ) {
        if (questionMap == null || questionMap.isEmpty()) return Collections.emptyList();

        Map<String, List<SurveyAnswerEntity>> byQ = answers.stream()
                .filter(a -> a.getSurveyQuestionId() != null)
                .collect(Collectors.groupingBy(SurveyAnswerEntity::getSurveyQuestionId));

        List<SurveyAnalyticsReportResponse.QuestionStat> out = new ArrayList<>();

        for (SurveyQuestionEntity q : questionMap.values()) {
            List<SurveyAnswerEntity> ans = byQ.getOrDefault(q.getSurveyQuestionId(), Collections.emptyList());

            SurveyAnalyticsReportResponse.QuestionStat stat = new SurveyAnalyticsReportResponse.QuestionStat();
            stat.setQuestionId(q.getSurveyQuestionId());
            stat.setQuestionText(q.getContent());
            stat.setQuestionType(q.getType());
            stat.setDimensionCode(q.getDimensionCode());
            stat.setResponseCount(ans.size());

            String type = q.getType() == null ? "" : q.getType().toUpperCase();

            if ("RATING".equals(type)) {
                List<Integer> ratings = ans.stream()
                        .map(SurveyAnswerEntity::getValueRating)
                        .filter(Objects::nonNull)
                        .toList();
                if (!ratings.isEmpty()) {
                    double a = ratings.stream().mapToInt(Integer::intValue).average().orElse(0);
                    stat.setAverageScore(BigDecimal.valueOf(a).setScale(2, RoundingMode.HALF_UP));
                }
            } else if ("CHOICE".equals(type) || "MULTIPLE_CHOICE".equals(type)) {
                Map<String, Long> dist = ans.stream()
                        .map(SurveyAnswerEntity::getValueChoice)
                        .filter(Objects::nonNull)
                        .collect(Collectors.groupingBy(x -> x, Collectors.counting()));
                stat.setChoiceDistribution(dist);
            } else if ("TEXT".equals(type)) {
                int textCnt = (int) ans.stream()
                        .map(SurveyAnswerEntity::getValueText)
                        .filter(v -> v != null && !v.isBlank())
                        .count();
                stat.setTextAnswerCount(textCnt);
            }

            out.add(stat);
        }

        // sort by sortOrder
        out.sort(Comparator.comparingInt(s -> {
            SurveyQuestionEntity q = questionMap.get(s.getQuestionId());
            return q != null && q.getSortOrder() != null ? q.getSortOrder() : 0;
        }));
        return out;
    }

    private List<SurveyAnalyticsReportResponse.DimensionStat> buildDimensionStats(
            Map<String, SurveyQuestionEntity> questionMap,
            List<SurveyAnswerEntity> answers
    ) {
        if (questionMap == null || questionMap.isEmpty()) return Collections.emptyList();

        // group answers by questionId
        Map<String, List<SurveyAnswerEntity>> byQ = answers.stream()
                .filter(a -> a.getSurveyQuestionId() != null)
                .collect(Collectors.groupingBy(SurveyAnswerEntity::getSurveyQuestionId));

        // dimension -> ratings list
        Map<String, List<Integer>> ratingsByDim = new HashMap<>();
        Map<String, Integer> questionCountByDim = new HashMap<>();
        Map<String, Integer> answerCountByDim = new HashMap<>();

        for (SurveyQuestionEntity q : questionMap.values()) {
            if (q == null) continue;
            if (q.getMeasurable() != null && !q.getMeasurable()) continue; // chỉ measurable
            if (q.getDimensionCode() == null || q.getDimensionCode().isBlank()) continue;
            if (q.getType() == null || !"RATING".equalsIgnoreCase(q.getType())) continue;

            String dim = q.getDimensionCode().trim();
            questionCountByDim.put(dim, questionCountByDim.getOrDefault(dim, 0) + 1);

            List<SurveyAnswerEntity> ans = byQ.getOrDefault(q.getSurveyQuestionId(), Collections.emptyList());
            answerCountByDim.put(dim, answerCountByDim.getOrDefault(dim, 0) + ans.size());

            List<Integer> ratings = ans.stream()
                    .map(SurveyAnswerEntity::getValueRating)
                    .filter(Objects::nonNull)
                    .toList();

            if (!ratings.isEmpty()) {
                ratingsByDim.computeIfAbsent(dim, k -> new ArrayList<>()).addAll(ratings);
            }
        }

        List<SurveyAnalyticsReportResponse.DimensionStat> out = new ArrayList<>();
        for (String dim : questionCountByDim.keySet()) {
            SurveyAnalyticsReportResponse.DimensionStat ds = new SurveyAnalyticsReportResponse.DimensionStat();
            ds.setDimensionCode(dim);
            ds.setQuestionCount(questionCountByDim.get(dim));
            ds.setResponseCount(answerCountByDim.getOrDefault(dim, 0));

            List<Integer> list = ratingsByDim.getOrDefault(dim, Collections.emptyList());
            if (!list.isEmpty()) {
                double a = list.stream().mapToInt(Integer::intValue).average().orElse(0);
                ds.setAverageScore(BigDecimal.valueOf(a).setScale(2, RoundingMode.HALF_UP));
            }

            out.add(ds);
        }

        // sort low score first (để HR thấy vấn đề)
        out.sort(Comparator.comparing(
                SurveyAnalyticsReportResponse.DimensionStat::getAverageScore,
                Comparator.nullsLast(BigDecimal::compareTo)
        ));
        return out;
    }

//    private List<SurveyAnalyticsReportResponse.StageTrend> buildStageTrends(List<SurveyResponseFilterRow> rows) {
//        // nếu SurveyResponseFilterRow có getStage() -> dùng
//        // còn nếu không có, tạm return empty cho khỏi phá.
//        try {
//            Map<String, List<SurveyResponseFilterRow>> byStage = rows.stream()
//                    .filter(r -> r.getStage() != null)
//                    .collect(Collectors.groupingBy(SurveyResponseFilterRow::getStage));
//
//            List<SurveyAnalyticsReportResponse.StageTrend> out = new ArrayList<>();
//            for (Map.Entry<String, List<SurveyResponseFilterRow>> e : byStage.entrySet()) {
//                String stage = e.getKey();
//                List<SurveyResponseFilterRow> rs = e.getValue();
//
//                SurveyAnalyticsReportResponse.StageTrend t = new SurveyAnalyticsReportResponse.StageTrend();
//                t.setStage(stage);
//                t.setSubmittedCount((int) rs.stream().map(SurveyResponseFilterRow::getSurveyResponseId).distinct().count());
//                t.setAverageOverall(avg(rs.stream()
//                        .map(SurveyResponseFilterRow::getOverallScore)
//                        .filter(Objects::nonNull)
//                        .toList()
//                ));
//                out.add(t);
//            }
//
//            out.sort(Comparator.comparing(SurveyAnalyticsReportResponse.StageTrend::getStage));
//            return out;
//        } catch (Exception ignore) {
//            return Collections.emptyList();
//        }
//    }
}