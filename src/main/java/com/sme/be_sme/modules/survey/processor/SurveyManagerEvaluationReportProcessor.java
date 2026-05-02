package com.sme.be_sme.modules.survey.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sme.be_sme.modules.survey.api.request.SurveyManagerEvaluationReportRequest;
import com.sme.be_sme.modules.survey.api.response.SurveyManagerEvaluationReportResponse;
import com.sme.be_sme.modules.survey.infrastructure.mapper.SurveyManagerEvaluationReportMapper;
import com.sme.be_sme.shared.constant.ErrorCodes;
import com.sme.be_sme.shared.exception.AppException;
import com.sme.be_sme.shared.gateway.core.BaseBizProcessor;
import com.sme.be_sme.shared.gateway.core.BizContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SurveyManagerEvaluationReportProcessor extends BaseBizProcessor<BizContext> {

    private final ObjectMapper objectMapper;
    private final SurveyManagerEvaluationReportMapper reportMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        SurveyManagerEvaluationReportRequest request = payload == null || payload.isNull()
                ? new SurveyManagerEvaluationReportRequest()
                : objectMapper.convertValue(payload, SurveyManagerEvaluationReportRequest.class);

        List<Map<String, Object>> evaluations = reportMapper.selectEvaluationRows(
                context.getTenantId(),
                trim(request.getTemplateId()),
                trim(request.getStartDate()),
                trim(request.getEndDate()),
                trim(request.getManagerUserId()),
                trim(request.getKeyword())
        );

        List<Map<String, Object>> answers = reportMapper.selectAnswerRows(
                context.getTenantId(),
                trim(request.getTemplateId()),
                trim(request.getStartDate()),
                trim(request.getEndDate()),
                trim(request.getManagerUserId()),
                trim(request.getKeyword())
        );

        SurveyManagerEvaluationReportResponse response = new SurveyManagerEvaluationReportResponse();
        response.setEmployees(buildEmployees(evaluations, answers));
        response.setSentCount(evaluations.size());
        response.setSubmittedCount((int) evaluations.stream().filter(this::hasResponse).count());
        response.setPendingCount(Math.max(response.getSentCount() - response.getSubmittedCount(), 0));
        response.setResponseRate(percent(response.getSubmittedCount(), response.getSentCount()));
        response.setAverageScore(avgOverall(evaluations, answers));
        response.setRecommendationRate(calculateRecommendationRate(answers));
        response.setDimensionStats(buildDimensionStats(answers));
        response.setQuestionStats(buildQuestionStats(answers, response.getSubmittedCount()));
        response.setRiskItems(buildInsights(response.getDimensionStats(), true));
        response.setStrengthItems(buildInsights(response.getDimensionStats(), false));

        return response;
    }

    private List<SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow> buildEmployees(
            List<Map<String, Object>> evaluations,
            List<Map<String, Object>> answers
    ) {
        Map<String, String> recommendationByResponseId = extractRecommendationByResponseId(answers);

        return evaluations.stream().map(row -> {
            SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow item =
                    new SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow();
            item.setSurveyInstanceId(str(row.get("survey_instance_id")));
            item.setSurveyResponseId(str(row.get("survey_response_id")));
            item.setOnboardingId(str(row.get("onboarding_id")));
            item.setTemplateId(str(row.get("survey_template_id")));
            item.setTemplateName(str(row.get("template_name")));
            item.setEmployeeUserId(str(row.get("employee_user_id")));
            item.setEmployeeName(str(row.get("employee_name")));
            item.setEmployeeEmail(str(row.get("employee_email")));
            item.setManagerUserId(str(row.get("manager_user_id")));
            item.setManagerName(str(row.get("manager_name")));
            item.setSentAt(date(row.get("sent_at")));
            item.setSubmittedAt(date(row.get("submitted_at")));
            item.setCompletedAt(date(row.get("completed_at")));
            item.setOverallScore(decimal(row.get("overall_score")));
            item.setRecommendation(recommendationByResponseId.get(item.getSurveyResponseId()));
            item.setStatus(resolveStatus(row));
            return item;
        }).toList();
    }

    private Map<String, String> extractRecommendationByResponseId(List<Map<String, Object>> answers) {
        Map<String, String> result = new LinkedHashMap<>();

        for (Map<String, Object> row : answers) {
            String responseId = str(row.get("survey_response_id"));
            String dimension = upper(str(row.get("dimension_code")));

            if (!StringUtils.hasText(responseId) || !"RECOMMENDATION".equals(dimension)) {
                continue;
            }

            String value = firstText(row.get("value_choice"), row.get("value_text"), row.get("value_choices"));
            if (StringUtils.hasText(value)) {
                result.putIfAbsent(responseId, value);
            }
        }

        return result;
    }

    private List<SurveyManagerEvaluationReportResponse.DimensionStat> buildDimensionStats(List<Map<String, Object>> answers) {
        Map<String, List<Map<String, Object>>> groups = answers.stream()
                .filter(row -> decimal(row.get("value_rating")) != null)
                .collect(Collectors.groupingBy(
                        row -> normalizeDimension(str(row.get("dimension_code"))),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<SurveyManagerEvaluationReportResponse.DimensionStat> result = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> entry : groups.entrySet()) {
            Set<String> questions = entry.getValue().stream()
                    .map(row -> str(row.get("survey_question_id")))
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            SurveyManagerEvaluationReportResponse.DimensionStat stat = new SurveyManagerEvaluationReportResponse.DimensionStat();
            stat.setDimensionCode(entry.getKey());
            stat.setQuestionCount(questions.size());
            stat.setResponseCount(entry.getValue().size());
            stat.setAverageScore(avg(entry.getValue().stream()
                    .map(row -> decimal(row.get("value_rating")))
                    .filter(Objects::nonNull)
                    .toList()));
            result.add(stat);
        }

        result.sort(Comparator.comparing(SurveyManagerEvaluationReportResponse.DimensionStat::getAverageScore));
        return result;
    }

    private List<SurveyManagerEvaluationReportResponse.QuestionStat> buildQuestionStats(
            List<Map<String, Object>> answers,
            int submittedCount
    ) {
        Map<String, List<Map<String, Object>>> groups = answers.stream()
                .filter(row -> StringUtils.hasText(str(row.get("survey_question_id"))))
                .collect(Collectors.groupingBy(
                        row -> str(row.get("survey_question_id")),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<SurveyManagerEvaluationReportResponse.QuestionStat> result = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> entry : groups.entrySet()) {
            List<Map<String, Object>> rows = entry.getValue();
            Map<String, Object> first = rows.get(0);

            SurveyManagerEvaluationReportResponse.QuestionStat stat = new SurveyManagerEvaluationReportResponse.QuestionStat();
            stat.setQuestionId(entry.getKey());
            stat.setContent(str(first.get("question_content")));
            stat.setType(str(first.get("question_type")));
            stat.setDimensionCode(normalizeDimension(str(first.get("dimension_code"))));
            stat.setResponseCount(rows.size());
            stat.setCompletionRate(percent(rows.size(), submittedCount));
            stat.setAverageScore(avg(rows.stream()
                    .map(row -> decimal(row.get("value_rating")))
                    .filter(Objects::nonNull)
                    .toList()));

            Map<String, Integer> choices = new LinkedHashMap<>();
            List<String> texts = new ArrayList<>();

            for (Map<String, Object> row : rows) {
                String choice = firstText(row.get("value_choice"), row.get("value_choices"));
                if (StringUtils.hasText(choice)) {
                    choices.merge(choice, 1, Integer::sum);
                }

                String text = str(row.get("value_text"));
                if (StringUtils.hasText(text) && texts.size() < 5) {
                    texts.add(text);
                }
            }

            stat.setChoiceDistribution(choices);
            stat.setTextAnswerCount(texts.size());
            stat.setSampleTexts(texts);
            result.add(stat);
        }

        return result;
    }

    private List<SurveyManagerEvaluationReportResponse.InsightItem> buildInsights(
            List<SurveyManagerEvaluationReportResponse.DimensionStat> dimensions,
            boolean lowest
    ) {
        Comparator<SurveyManagerEvaluationReportResponse.DimensionStat> comparator =
                Comparator.comparing(SurveyManagerEvaluationReportResponse.DimensionStat::getAverageScore);

        return dimensions.stream()
                .sorted(lowest ? comparator : comparator.reversed())
                .limit(3)
                .map(item -> {
                    SurveyManagerEvaluationReportResponse.InsightItem insight = new SurveyManagerEvaluationReportResponse.InsightItem();
                    insight.setLabel(item.getDimensionCode());
                    insight.setValue(item.getAverageScore());
                    insight.setSubtext(item.getResponseCount() + " responses");
                    return insight;
                })
                .toList();
    }

    private BigDecimal avgOverall(List<Map<String, Object>> evaluations, List<Map<String, Object>> answers) {
        List<BigDecimal> values = evaluations.stream()
                .map(row -> decimal(row.get("overall_score")))
                .filter(Objects::nonNull)
                .toList();

        if (!values.isEmpty()) {
            return avg(values);
        }

        return avg(answers.stream()
                .map(row -> decimal(row.get("value_rating")))
                .filter(Objects::nonNull)
                .toList());
    }

    private BigDecimal calculateRecommendationRate(List<Map<String, Object>> answers) {
        int total = 0;
        int recommended = 0;

        for (Map<String, Object> row : answers) {
            if (!"RECOMMENDATION".equals(normalizeDimension(str(row.get("dimension_code"))))) {
                continue;
            }

            String value = firstText(row.get("value_choice"), row.get("value_text"), row.get("value_choices"));
            if (!StringUtils.hasText(value)) {
                BigDecimal rating = decimal(row.get("value_rating"));
                if (rating == null) continue;
                total++;
                if (rating.compareTo(BigDecimal.valueOf(4)) >= 0) recommended++;
                continue;
            }

            total++;
            String lower = value.toLowerCase(Locale.ROOT);
            if (lower.contains("tiếp tục") || lower.contains("chính thức") || lower.contains("recommend") || lower.contains("yes")) {
                recommended++;
            }
        }

        return percent(recommended, total);
    }

    private boolean hasResponse(Map<String, Object> row) {
        return StringUtils.hasText(str(row.get("survey_response_id")));
    }

    private String resolveStatus(Map<String, Object> row) {
        if (hasResponse(row)) {
            return "SUBMITTED";
        }

        String status = upper(str(row.get("survey_status")));
        if ("EXPIRED".equals(status)) return "EXPIRED";
        if ("SENT".equals(status) || "SCHEDULED".equals(status) || "PENDING".equals(status)) return "PENDING";
        return StringUtils.hasText(status) ? status : "PENDING";
    }

    private static String normalizeDimension(String value) {
        String raw = upper(value);
        return StringUtils.hasText(raw) ? raw : "OVERALL_COMMENT";
    }

    private static BigDecimal avg(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(int numerator, int denominator) {
        if (denominator <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private static String firstText(Object... values) {
        for (Object value : values) {
            String text = str(value);
            if (StringUtils.hasText(text)) return text;
        }
        return null;
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        try {
            return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Date date(Object value) {
        if (value == null) return null;
        if (value instanceof Date date) return date;
        if (value instanceof Timestamp timestamp) return new Date(timestamp.getTime());
        return null;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String upper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }
}
