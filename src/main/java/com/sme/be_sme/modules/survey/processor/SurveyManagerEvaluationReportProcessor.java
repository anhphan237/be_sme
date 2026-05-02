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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SurveyManagerEvaluationReportProcessor extends BaseBizProcessor<BizContext> {

    private static final String FIT = "FIT";
    private static final String FOLLOW_UP = "FOLLOW_UP";
    private static final String NOT_FIT = "NOT_FIT";
    private static final String NOT_EVALUATED = "NOT_EVALUATED";

    private final ObjectMapper objectMapper;
    private final SurveyManagerEvaluationReportMapper reportMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }

        SurveyManagerEvaluationReportRequest request =
                payload == null || payload.isNull()
                        ? new SurveyManagerEvaluationReportRequest()
                        : objectMapper.convertValue(payload, SurveyManagerEvaluationReportRequest.class);

        List<Map<String, Object>> evaluations = reportMapper.selectEvaluationRows(
                context.getTenantId(),
                trim(request.getTemplateId()),
                trim(request.getStartDate()),
                trim(request.getEndDate()),
                trim(request.getManagerUserId()),
                trim(request.getKeyword()),
                upper(trim(request.getStatus()))
        );

        List<Map<String, Object>> answers = reportMapper.selectAnswerRows(
                context.getTenantId(),
                trim(request.getTemplateId()),
                trim(request.getStartDate()),
                trim(request.getEndDate()),
                trim(request.getManagerUserId()),
                trim(request.getKeyword()),
                upper(trim(request.getStatus()))
        );

        List<SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow> employees =
                buildEmployees(evaluations, answers);

        String fitFilter = upper(trim(request.getFitLevel()));
        if (StringUtils.hasText(fitFilter)) {
            employees = employees.stream()
                    .filter(item -> fitFilter.equals(upper(item.getFitLevel())))
                    .toList();
        }

        SurveyManagerEvaluationReportResponse response = new SurveyManagerEvaluationReportResponse();
        response.setEmployees(employees);
        response.setSummary(buildSummary(employees));
        return response;
    }

    private SurveyManagerEvaluationReportResponse.Summary buildSummary(
            List<SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow> employees
    ) {
        SurveyManagerEvaluationReportResponse.Summary summary =
                new SurveyManagerEvaluationReportResponse.Summary();

        int total = employees.size();

        int submitted = (int) employees.stream()
                .filter(item -> "SUBMITTED".equals(upper(item.getStatus())))
                .count();

        int pending = Math.max(total - submitted, 0);

        summary.setTotalEmployees(total);
        summary.setSentCount(total);
        summary.setSubmittedCount(submitted);
        summary.setPendingCount(pending);
        summary.setResponseRate(percent(submitted, total));


        summary.setAverageScore(avg(
                employees.stream()
                        .filter(item -> "SUBMITTED".equals(upper(item.getStatus())))
                        .map(SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow::getAverageScore)
                        .filter(Objects::nonNull)
                        .filter(score -> score.compareTo(BigDecimal.ZERO) > 0)
                        .toList()
        ));

        summary.setFitCount((int) employees.stream()
                .filter(item -> FIT.equals(item.getFitLevel()))
                .count());

        summary.setNeedFollowUpCount((int) employees.stream()
                .filter(item -> FOLLOW_UP.equals(item.getFitLevel()))
                .count());

        summary.setNotFitCount((int) employees.stream()
                .filter(item -> NOT_FIT.equals(item.getFitLevel()))
                .count());

        summary.setNotEvaluatedCount((int) employees.stream()
                .filter(item -> NOT_EVALUATED.equals(item.getFitLevel()))
                .count());

        return summary;
    }

    private List<SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow> buildEmployees(
            List<Map<String, Object>> evaluations,
            List<Map<String, Object>> answers
    ) {
        Map<String, List<Map<String, Object>>> answersByResponseId = answers.stream()
                .filter(row -> StringUtils.hasText(str(row.get("survey_response_id"))))
                .collect(Collectors.groupingBy(
                        row -> str(row.get("survey_response_id")),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return evaluations.stream().map(row -> {
            SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow item =
                    new SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow();

            String responseId = str(row.get("survey_response_id"));
            List<Map<String, Object>> responseAnswers =
                    answersByResponseId.getOrDefault(responseId, List.of());

            BigDecimal averageScore = StringUtils.hasText(responseId)
                    ? resolveAverageScore(row, responseAnswers)
                    : null;

            String recommendation = StringUtils.hasText(responseId)
                    ? extractRecommendation(responseAnswers)
                    : null;

            String fitLevel = resolveFitLevel(averageScore, recommendation, responseId);

            item.setSurveyInstanceId(str(row.get("survey_instance_id")));
            item.setSurveyResponseId(responseId);
            item.setOnboardingId(str(row.get("onboarding_id")));
            item.setEmployeeUserId(str(row.get("employee_user_id")));
            item.setEmployeeName(str(row.get("employee_name")));
            item.setEmployeeEmail(str(row.get("employee_email")));
            item.setJobTitle(str(row.get("job_title")));
            item.setDepartmentName(str(row.get("department_name")));
            item.setManagerUserId(str(row.get("manager_user_id")));
            item.setManagerName(str(row.get("manager_name")));
            item.setManagerEmail(str(row.get("manager_email")));
            item.setStatus(resolveStatus(row));
            item.setAverageScore(averageScore);
            item.setRecommendation(recommendation);
            item.setRecommendationLabel(toRecommendationLabel(recommendation));
            item.setFitLevel(fitLevel);
            item.setFitLabel(toFitLabel(fitLevel));
            item.setSentAt(date(row.get("sent_at")));
            item.setSubmittedAt(date(row.get("submitted_at")));
            item.setCompletedAt(date(row.get("completed_at")));
            item.setDimensionScores(buildDimensionScores(responseAnswers));
            item.setTextFeedbacks(buildTextFeedbacks(responseAnswers));

            return item;
        }).toList();
    }

    private BigDecimal resolveAverageScore(Map<String, Object> row, List<Map<String, Object>> answers) {
        BigDecimal storedOverall = decimal(row.get("overall_score"));
        if (storedOverall != null && storedOverall.compareTo(BigDecimal.ZERO) > 0) {
            return storedOverall;
        }

        return avg(answers.stream()
                .map(answer -> decimal(answer.get("value_rating")))
                .filter(Objects::nonNull)
                .toList());
    }

    private List<SurveyManagerEvaluationReportResponse.DimensionScore> buildDimensionScores(
            List<Map<String, Object>> answers
    ) {
        Map<String, List<BigDecimal>> grouped = new LinkedHashMap<>();

        for (Map<String, Object> row : answers) {
            BigDecimal rating = decimal(row.get("value_rating"));
            if (rating == null) continue;

            String dimensionCode = normalizeDimension(str(row.get("dimension_code")));
            grouped.computeIfAbsent(dimensionCode, ignored -> new ArrayList<>()).add(rating);
        }

        return grouped.entrySet().stream()
                .map(entry -> {
                    SurveyManagerEvaluationReportResponse.DimensionScore score =
                            new SurveyManagerEvaluationReportResponse.DimensionScore();
                    score.setDimensionCode(entry.getKey());
                    score.setDimensionName(toDimensionName(entry.getKey()));
                    score.setScore(avg(entry.getValue()));
                    return score;
                })
                .sorted(Comparator.comparing(SurveyManagerEvaluationReportResponse.DimensionScore::getDimensionCode))
                .toList();
    }

    private List<SurveyManagerEvaluationReportResponse.TextFeedback> buildTextFeedbacks(
            List<Map<String, Object>> answers
    ) {
        List<SurveyManagerEvaluationReportResponse.TextFeedback> result = new ArrayList<>();

        for (Map<String, Object> row : answers) {
            String text = str(row.get("value_text"));
            if (!StringUtils.hasText(text)) continue;

            SurveyManagerEvaluationReportResponse.TextFeedback feedback =
                    new SurveyManagerEvaluationReportResponse.TextFeedback();
            feedback.setQuestion(str(row.get("question_content")));
            feedback.setAnswer(text.trim());
            result.add(feedback);
        }

        return result;
    }

    private String extractRecommendation(List<Map<String, Object>> answers) {
        for (Map<String, Object> row : answers) {
            if (!"RECOMMENDATION".equals(normalizeDimension(str(row.get("dimension_code"))))) {
                continue;
            }

            String value = firstText(row.get("value_choice"), row.get("value_text"), row.get("value_choices"));
            if (StringUtils.hasText(value)) return value.trim();

            BigDecimal rating = decimal(row.get("value_rating"));
            if (rating != null) return rating.toPlainString();
        }

        return null;
    }

    private String resolveFitLevel(BigDecimal averageScore, String recommendation, String responseId) {
        if (!StringUtils.hasText(responseId)) {
            return NOT_EVALUATED;
        }

        String rec = lower(recommendation);

        if (rec.contains("không") || rec.contains("not fit") || rec.contains("reject") || rec.contains("không phù hợp")) {
            return NOT_FIT;
        }

        if (rec.contains("theo dõi") || rec.contains("follow") || rec.contains("cần thêm")) {
            return FOLLOW_UP;
        }

        if (averageScore == null || averageScore.compareTo(BigDecimal.ZERO) == 0) {
            return FOLLOW_UP;
        }

        if (averageScore.compareTo(BigDecimal.valueOf(3)) < 0) {
            return NOT_FIT;
        }

        if (averageScore.compareTo(BigDecimal.valueOf(4)) < 0) {
            return FOLLOW_UP;
        }

        return FIT;
    }

    private String resolveStatus(Map<String, Object> row) {
        if (StringUtils.hasText(str(row.get("survey_response_id")))) {
            return "SUBMITTED";
        }

        String status = upper(str(row.get("survey_status")));
        if ("EXPIRED".equals(status)) return "EXPIRED";
        if ("SENT".equals(status) || "SCHEDULED".equals(status) || "PENDING".equals(status)) return "PENDING";
        return StringUtils.hasText(status) ? status : "PENDING";
    }

    private static String normalizeDimension(String value) {
        String raw = upper(value);
        if (!StringUtils.hasText(raw)) return "OVERALL_COMMENT";

        return switch (raw) {
            case "ROLE_FIT",
                 "WORK_QUALITY",
                 "LEARNING_ABILITY",
                 "PROACTIVENESS",
                 "TEAM_INTEGRATION",
                 "ATTITUDE_CULTURE",
                 "RECOMMENDATION",
                 "OVERALL_COMMENT" -> raw;
            default -> "OVERALL_COMMENT";
        };
    }

    private static String toDimensionName(String dimensionCode) {
        return switch (normalizeDimension(dimensionCode)) {
            case "ROLE_FIT" -> "Mức độ phù hợp với vị trí";
            case "WORK_QUALITY" -> "Chất lượng thực hiện công việc";
            case "LEARNING_ABILITY" -> "Khả năng tiếp thu và thích nghi";
            case "PROACTIVENESS" -> "Mức độ chủ động";
            case "TEAM_INTEGRATION" -> "Khả năng hòa nhập đội nhóm";
            case "ATTITUDE_CULTURE" -> "Thái độ và phù hợp văn hóa";
            case "RECOMMENDATION" -> "Đề xuất tiếp tục chính thức";
            default -> "Nhận xét tổng quan";
        };
    }

    private static String toFitLabel(String fitLevel) {
        return switch (upper(fitLevel)) {
            case FIT -> "Phù hợp";
            case FOLLOW_UP -> "Cần theo dõi";
            case NOT_FIT -> "Không phù hợp";
            default -> "Chưa đánh giá";
        };
    }

    private static String toRecommendationLabel(String recommendation) {
        if (!StringUtils.hasText(recommendation)) return null;

        String rec = lower(recommendation);

        if (rec.contains("tiếp tục") || rec.contains("chính thức") || rec.contains("continue") || rec.contains("yes")) {
            return "Đề xuất tiếp tục chính thức";
        }

        if (rec.contains("theo dõi") || rec.contains("follow")) {
            return "Cần theo dõi thêm";
        }

        if (rec.contains("không") || rec.contains("not")) {
            return "Không phù hợp";
        }

        return recommendation;
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

    private static BigDecimal decimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }

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

    private static String firstText(Object... values) {
        for (Object value : values) {
            String text = str(value);
            if (StringUtils.hasText(text)) return text;
        }
        return null;
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String upper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private static String lower(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }
}
