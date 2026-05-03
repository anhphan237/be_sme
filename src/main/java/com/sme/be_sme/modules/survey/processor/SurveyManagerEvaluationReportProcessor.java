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
import java.util.Collections;
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

    private static final String CONTINUE_OFFICIAL = "CONTINUE_OFFICIAL";
    private static final String EXTEND_PROBATION = "EXTEND_PROBATION";
    private static final String NEED_TRAINING = "NEED_TRAINING";
    private static final String NOT_CONTINUE = "NOT_CONTINUE";
    private static final String UNKNOWN = "UNKNOWN";

    private static final BigDecimal FIT_SCORE = BigDecimal.valueOf(4);
    private static final BigDecimal FOLLOW_UP_SCORE = BigDecimal.valueOf(3);
    private static final BigDecimal GOOD_DIMENSION_SCORE = BigDecimal.valueOf(4);
    private static final BigDecimal LOW_DIMENSION_SCORE = BigDecimal.valueOf(3);

    private final ObjectMapper objectMapper;
    private final SurveyManagerEvaluationReportMapper reportMapper;

    @Override
    protected Object doProcess(BizContext context, JsonNode payload) {
        validate(context);

        SurveyManagerEvaluationReportRequest request =
                payload == null || payload.isNull()
                        ? new SurveyManagerEvaluationReportRequest()
                        : objectMapper.convertValue(payload, SurveyManagerEvaluationReportRequest.class);

        List<Map<String, Object>> evaluations = safeRows(reportMapper.selectEvaluationRows(
                context.getTenantId(),
                trim(request.getTemplateId()),
                trim(request.getStartDate()),
                trim(request.getEndDate()),
                trim(request.getManagerUserId()),
                trim(request.getKeyword()),
                upper(trim(request.getStatus()))
        ));

        List<Map<String, Object>> answers = safeRows(reportMapper.selectAnswerRows(
                context.getTenantId(),
                trim(request.getTemplateId()),
                trim(request.getStartDate()),
                trim(request.getEndDate()),
                trim(request.getManagerUserId()),
                trim(request.getKeyword()),
                upper(trim(request.getStatus()))
        ));

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
        response.setManagerEvaluationInsights(employees);
        response.setSummary(buildSummary(employees));

        return response;
    }

    private void validate(BizContext context) {
        if (context == null || !StringUtils.hasText(context.getTenantId())) {
            throw AppException.of(ErrorCodes.BAD_REQUEST, "tenantId is required");
        }
    }

    private SurveyManagerEvaluationReportResponse.Summary buildSummary(
            List<SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow> employees
    ) {
        List<SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow> safeEmployees =
                employees == null ? Collections.emptyList() : employees;

        SurveyManagerEvaluationReportResponse.Summary summary =
                new SurveyManagerEvaluationReportResponse.Summary();

        int total = safeEmployees.size();

        int submitted = (int) safeEmployees.stream()
                .filter(item -> "SUBMITTED".equals(upper(item.getStatus())))
                .count();

        int pending = Math.max(total - submitted, 0);

        summary.setTotalEmployees(total);
        summary.setSentCount(total);
        summary.setSubmittedCount(submitted);
        summary.setPendingCount(pending);
        summary.setResponseRate(percent(submitted, total));

        summary.setAverageScore(avgNullable(
                safeEmployees.stream()
                        .filter(item -> "SUBMITTED".equals(upper(item.getStatus())))
                        .map(SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow::getAverageScore)
                        .filter(Objects::nonNull)
                        .filter(score -> score.compareTo(BigDecimal.ZERO) > 0)
                        .toList()
        ));

        summary.setFitCount((int) safeEmployees.stream()
                .filter(item -> FIT.equals(upper(item.getFitLevel())))
                .count());

        summary.setNeedFollowUpCount((int) safeEmployees.stream()
                .filter(item -> FOLLOW_UP.equals(upper(item.getFitLevel())))
                .count());

        summary.setNotFitCount((int) safeEmployees.stream()
                .filter(item -> NOT_FIT.equals(upper(item.getFitLevel())))
                .count());

        summary.setNotEvaluatedCount((int) safeEmployees.stream()
                .filter(item -> NOT_EVALUATED.equals(upper(item.getFitLevel())))
                .count());

        summary.setOfficialRecommendedCount((int) safeEmployees.stream()
                .filter(item -> CONTINUE_OFFICIAL.equals(upper(item.getRecommendationDecision())))
                .count());

        summary.setTrainingRecommendedCount((int) safeEmployees.stream()
                .filter(item -> NEED_TRAINING.equals(upper(item.getRecommendationDecision())))
                .count());

        summary.setExtendProbationCount((int) safeEmployees.stream()
                .filter(item -> EXTEND_PROBATION.equals(upper(item.getRecommendationDecision())))
                .count());

        summary.setNotContinueCount((int) safeEmployees.stream()
                .filter(item -> NOT_CONTINUE.equals(upper(item.getRecommendationDecision())))
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

        return evaluations.stream()
                .map(row -> buildEmployee(row, answersByResponseId))
                .sorted(this::compareEmployeeEvaluation)
                .toList();
    }

    private SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow buildEmployee(
            Map<String, Object> row,
            Map<String, List<Map<String, Object>>> answersByResponseId
    ) {
        SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow item =
                new SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow();

        String responseId = str(row.get("survey_response_id"));
        List<Map<String, Object>> responseAnswers =
                answersByResponseId.getOrDefault(responseId, Collections.emptyList());

        boolean submitted = StringUtils.hasText(responseId);

        BigDecimal averageScore = submitted
                ? resolveAverageScore(row, responseAnswers)
                : null;

        String rawRecommendation = submitted
                ? extractRecommendationRaw(responseAnswers)
                : null;

        String recommendationDecision = submitted
                ? resolveRecommendationDecision(rawRecommendation, responseAnswers, averageScore)
                : UNKNOWN;

        String fitLevel = resolveFitLevel(averageScore, recommendationDecision, responseId);

        List<SurveyManagerEvaluationReportResponse.DimensionScore> dimensionScores =
                buildDimensionScores(responseAnswers);

        List<SurveyManagerEvaluationReportResponse.DimensionScore> weakDimensions =
                dimensionScores.stream()
                        .filter(score -> score.getScore() != null)
                        .filter(score -> score.getScore().compareTo(GOOD_DIMENSION_SCORE) < 0)
                        .sorted(Comparator.comparing(SurveyManagerEvaluationReportResponse.DimensionScore::getScore))
                        .limit(3)
                        .toList();

        List<SurveyManagerEvaluationReportResponse.DimensionScore> strongDimensions =
                dimensionScores.stream()
                        .filter(score -> score.getScore() != null)
                        .sorted(Comparator.comparing(
                                SurveyManagerEvaluationReportResponse.DimensionScore::getScore,
                                Comparator.reverseOrder()
                        ))
                        .limit(3)
                        .toList();

        List<SurveyManagerEvaluationReportResponse.TextFeedback> textFeedbacks =
                buildTextFeedbacks(responseAnswers);

        List<SurveyManagerEvaluationReportResponse.AnswerDetail> answerDetails =
                buildAnswerDetails(responseAnswers);

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

        item.setRecommendation(rawRecommendation);
        item.setRecommendationDecision(recommendationDecision);
        item.setRecommendationLabel(toRecommendationLabel(recommendationDecision, rawRecommendation));

        item.setFitLevel(fitLevel);
        item.setFitLabel(toFitLabel(fitLevel));

        item.setSentAt(date(row.get("sent_at")));
        item.setSubmittedAt(date(row.get("submitted_at")));
        item.setCompletedAt(date(row.get("completed_at")));

        item.setDimensionScores(dimensionScores);
        item.setWeakDimensions(weakDimensions);
        item.setStrongDimensions(strongDimensions);

        item.setTextFeedbacks(textFeedbacks);
        item.setAnswerDetails(answerDetails);

        item.setStrengths(buildStrengths(strongDimensions));
        item.setImprovementAreas(buildImprovementAreas(weakDimensions));
        item.setOverallComments(extractOverallComments(textFeedbacks));

        item.setSummary(buildEmployeeSummary(item));
        item.setActionRecommendation(buildActionRecommendation(item));

        return item;
    }

    private int compareEmployeeEvaluation(
            SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow a,
            SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow b
    ) {
        int fitCompare = Integer.compare(fitOrder(a.getFitLevel()), fitOrder(b.getFitLevel()));
        if (fitCompare != 0) {
            return fitCompare;
        }

        BigDecimal aScore = a.getAverageScore() == null ? BigDecimal.valueOf(999) : a.getAverageScore();
        BigDecimal bScore = b.getAverageScore() == null ? BigDecimal.valueOf(999) : b.getAverageScore();

        int scoreCompare = aScore.compareTo(bScore);
        if (scoreCompare != 0) {
            return scoreCompare;
        }

        Date aDate = a.getSubmittedAt();
        Date bDate = b.getSubmittedAt();

        if (aDate == null && bDate == null) return 0;
        if (aDate == null) return 1;
        if (bDate == null) return -1;

        return bDate.compareTo(aDate);
    }

    private int fitOrder(String fitLevel) {
        return switch (upper(fitLevel)) {
            case NOT_FIT -> 1;
            case FOLLOW_UP -> 2;
            case NOT_EVALUATED -> 3;
            case FIT -> 4;
            default -> 99;
        };
    }

    private BigDecimal resolveAverageScore(Map<String, Object> row, List<Map<String, Object>> answers) {
        BigDecimal storedOverall = decimal(row.get("overall_score"));
        if (storedOverall != null && storedOverall.compareTo(BigDecimal.ZERO) > 0) {
            return storedOverall;
        }

        return avgNullable(answers.stream()
                .filter(answer -> isScoringDimension(normalizeDimension(str(answer.get("dimension_code")))))
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
            if (!isScoringDimension(dimensionCode)) continue;

            grouped.computeIfAbsent(dimensionCode, ignored -> new ArrayList<>()).add(rating);
        }

        return grouped.entrySet().stream()
                .map(entry -> {
                    BigDecimal score = avgNullable(entry.getValue());

                    SurveyManagerEvaluationReportResponse.DimensionScore item =
                            new SurveyManagerEvaluationReportResponse.DimensionScore();

                    item.setDimensionCode(entry.getKey());
                    item.setDimensionName(toDimensionName(entry.getKey()));
                    item.setScore(score);
                    item.setAnswerCount(entry.getValue().size());
                    item.setLevel(toDimensionLevel(score));

                    return item;
                })
                .sorted(Comparator.comparing(SurveyManagerEvaluationReportResponse.DimensionScore::getDimensionCode))
                .toList();
    }

    private List<SurveyManagerEvaluationReportResponse.TextFeedback> buildTextFeedbacks(
            List<Map<String, Object>> answers
    ) {
        List<SurveyManagerEvaluationReportResponse.TextFeedback> result = new ArrayList<>();

        for (Map<String, Object> row : answers) {
            String text = firstText(row.get("value_text"), row.get("value_choice"), row.get("value_choices"));
            if (!StringUtils.hasText(text)) continue;

            String dimensionCode = normalizeDimension(str(row.get("dimension_code")));
            String questionType = upper(str(row.get("question_type")));

            if (!"TEXT".equals(questionType)) {
                continue;
            }

            SurveyManagerEvaluationReportResponse.TextFeedback feedback =
                    new SurveyManagerEvaluationReportResponse.TextFeedback();

            feedback.setQuestionId(str(row.get("survey_question_id")));
            feedback.setQuestion(str(row.get("question_content")));
            feedback.setAnswer(text.trim());
            feedback.setDimensionCode(dimensionCode);
            feedback.setDimensionName(toDimensionName(dimensionCode));
            feedback.setCategory(toTextFeedbackCategory(dimensionCode, text));

            result.add(feedback);
        }

        return result;
    }

    private List<SurveyManagerEvaluationReportResponse.AnswerDetail> buildAnswerDetails(
            List<Map<String, Object>> answers
    ) {
        List<SurveyManagerEvaluationReportResponse.AnswerDetail> result = new ArrayList<>();

        for (Map<String, Object> row : answers) {
            String dimensionCode = normalizeDimension(str(row.get("dimension_code")));
            String questionType = upper(str(row.get("question_type")));

            SurveyManagerEvaluationReportResponse.AnswerDetail detail =
                    new SurveyManagerEvaluationReportResponse.AnswerDetail();

            detail.setQuestionId(str(row.get("survey_question_id")));
            detail.setQuestion(str(row.get("question_content")));
            detail.setQuestionType(questionType);
            detail.setDimensionCode(dimensionCode);
            detail.setDimensionName(toDimensionName(dimensionCode));
            detail.setValueRating(integer(row.get("value_rating")));
            detail.setValueText(str(row.get("value_text")));
            detail.setValueChoice(str(row.get("value_choice")));
            detail.setValueChoices(parseChoices(row.get("value_choices"), row.get("value_text")));

            result.add(detail);
        }

        return result;
    }

    private String extractRecommendationRaw(List<Map<String, Object>> answers) {
        for (Map<String, Object> row : answers) {
            String dimensionCode = normalizeDimension(str(row.get("dimension_code")));
            if (!"RECOMMENDATION".equals(dimensionCode)) {
                continue;
            }

            String value = firstText(row.get("value_choice"), row.get("value_text"), row.get("value_choices"));
            if (StringUtils.hasText(value)) {
                return value.trim();
            }

            BigDecimal rating = decimal(row.get("value_rating"));
            if (rating != null) {
                return rating.toPlainString();
            }
        }

        return null;
    }

    private String resolveRecommendationDecision(
            String rawRecommendation,
            List<Map<String, Object>> answers,
            BigDecimal averageScore
    ) {
        String rec = lower(rawRecommendation);

        if (containsAny(rec, "không tiếp tục", "không phù hợp", "không đạt", "not continue", "not fit", "reject", "stop")) {
            return NOT_CONTINUE;
        }

        if (containsAny(rec, "đào tạo", "huấn luyện", "training", "coach", "mentor", "kèm thêm", "cần đào tạo")) {
            return NEED_TRAINING;
        }

        if (containsAny(rec, "theo dõi", "cần thêm thời gian", "gia hạn", "extend", "follow", "probation")) {
            return EXTEND_PROBATION;
        }

        if (containsAny(rec, "tiếp tục", "chính thức", "phù hợp", "continue", "official", "fit", "yes", "đạt")) {
            return CONTINUE_OFFICIAL;
        }

        for (Map<String, Object> row : answers) {
            String dimensionCode = normalizeDimension(str(row.get("dimension_code")));
            if (!"RECOMMENDATION".equals(dimensionCode)) {
                continue;
            }

            BigDecimal rating = decimal(row.get("value_rating"));
            if (rating == null) {
                continue;
            }

            if (rating.compareTo(BigDecimal.valueOf(4)) >= 0) {
                return CONTINUE_OFFICIAL;
            }

            if (rating.compareTo(BigDecimal.valueOf(3)) >= 0) {
                return EXTEND_PROBATION;
            }

            return NOT_CONTINUE;
        }

        if (averageScore == null || averageScore.compareTo(BigDecimal.ZERO) <= 0) {
            return UNKNOWN;
        }

        if (averageScore.compareTo(FIT_SCORE) >= 0) {
            return CONTINUE_OFFICIAL;
        }

        if (averageScore.compareTo(FOLLOW_UP_SCORE) >= 0) {
            return EXTEND_PROBATION;
        }

        return NOT_CONTINUE;
    }

    private String resolveFitLevel(BigDecimal averageScore, String recommendationDecision, String responseId) {
        if (!StringUtils.hasText(responseId)) {
            return NOT_EVALUATED;
        }

        String decision = upper(recommendationDecision);

        if (NOT_CONTINUE.equals(decision)) {
            return NOT_FIT;
        }

        if (NEED_TRAINING.equals(decision) || EXTEND_PROBATION.equals(decision)) {
            return FOLLOW_UP;
        }

        if (CONTINUE_OFFICIAL.equals(decision)) {
            if (averageScore == null || averageScore.compareTo(FOLLOW_UP_SCORE) < 0) {
                return FOLLOW_UP;
            }

            return FIT;
        }

        if (averageScore == null || averageScore.compareTo(BigDecimal.ZERO) <= 0) {
            return FOLLOW_UP;
        }

        if (averageScore.compareTo(FOLLOW_UP_SCORE) < 0) {
            return NOT_FIT;
        }

        if (averageScore.compareTo(FIT_SCORE) < 0) {
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

    private String buildEmployeeSummary(SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow item) {
        if (!"SUBMITTED".equals(upper(item.getStatus()))) {
            return String.format(
                    "%s chưa được quản lý hoàn tất đánh giá sau onboarding.",
                    safeText(item.getEmployeeName(), "Nhân viên")
            );
        }

        String employeeName = safeText(item.getEmployeeName(), "Nhân viên");
        String score = formatScore(item.getAverageScore());
        String fitLabel = safeText(item.getFitLabel(), "Chưa đánh giá");
        String recommendationLabel = safeText(item.getRecommendationLabel(), "Chưa có đề xuất");
        String weak = formatDimensionNames(item.getWeakDimensions());
        String strong = formatDimensionNames(item.getStrongDimensions());

        if (NOT_FIT.equals(upper(item.getFitLevel()))) {
            return String.format(
                    "%s được quản lý đánh giá là %s sau onboarding. Điểm trung bình %s/5. Nhóm cần xem xét chính: %s. Kết luận đề xuất: %s.",
                    employeeName,
                    fitLabel.toLowerCase(Locale.ROOT),
                    score,
                    weak,
                    recommendationLabel
            );
        }

        if (FOLLOW_UP.equals(upper(item.getFitLevel()))) {
            return String.format(
                    "%s cần được theo dõi thêm sau onboarding. Điểm trung bình %s/5. Nhóm cần cải thiện: %s. Nhóm thể hiện tốt: %s. Kết luận đề xuất: %s.",
                    employeeName,
                    score,
                    weak,
                    strong,
                    recommendationLabel
            );
        }

        if (FIT.equals(upper(item.getFitLevel()))) {
            return String.format(
                    "%s được quản lý đánh giá phù hợp để tiếp tục sau onboarding. Điểm trung bình %s/5. Nhóm thể hiện tốt: %s. Kết luận đề xuất: %s.",
                    employeeName,
                    score,
                    strong,
                    recommendationLabel
            );
        }

        return String.format(
                "%s chưa có đủ dữ liệu đánh giá sau onboarding để đưa ra kết luận nhân sự.",
                employeeName
        );
    }

    private String buildActionRecommendation(SurveyManagerEvaluationReportResponse.EmployeeEvaluationRow item) {
        if (!"SUBMITTED".equals(upper(item.getStatus()))) {
            return "HR nên nhắc quản lý hoàn tất đánh giá sau onboarding để có cơ sở ra quyết định nhân sự.";
        }

        String fitLevel = upper(item.getFitLevel());
        String decision = upper(item.getRecommendationDecision());
        String weak = formatDimensionNames(item.getWeakDimensions());

        if (NOT_FIT.equals(fitLevel) || NOT_CONTINUE.equals(decision)) {
            return "HR nên trao đổi với quản lý để xác nhận nguyên nhân chưa phù hợp, rà soát minh chứng trong quá trình onboarding và chuẩn bị phương án không tiếp tục hoặc chuyển hướng phù hợp.";
        }

        if (NEED_TRAINING.equals(decision)) {
            return String.format(
                    "HR nên lập kế hoạch đào tạo bổ sung cho nhân viên, tập trung vào các nhóm: %s. Sau đào tạo nên có một lần đánh giá lại.",
                    weak
            );
        }

        if (EXTEND_PROBATION.equals(decision) || FOLLOW_UP.equals(fitLevel)) {
            return String.format(
                    "HR nên thống nhất với quản lý một kế hoạch theo dõi thêm trong 1–2 tuần, tập trung vào các nhóm: %s. Cần có mục tiêu cải thiện rõ ràng trước khi ra quyết định chính thức.",
                    weak
            );
        }

        if (FIT.equals(fitLevel) || CONTINUE_OFFICIAL.equals(decision)) {
            return "HR có thể tiến hành các bước xác nhận tiếp tục chính thức, đồng thời ghi nhận các điểm mạnh của nhân viên để hỗ trợ lộ trình phát triển sau onboarding.";
        }

        return "HR nên kiểm tra lại nội dung đánh giá và trao đổi thêm với quản lý trước khi đưa ra quyết định cuối cùng.";
    }

    private List<String> buildStrengths(List<SurveyManagerEvaluationReportResponse.DimensionScore> strongDimensions) {
        if (strongDimensions == null || strongDimensions.isEmpty()) {
            return Collections.emptyList();
        }

        return strongDimensions.stream()
                .filter(item -> item.getScore() != null && item.getScore().compareTo(GOOD_DIMENSION_SCORE) >= 0)
                .map(item -> item.getDimensionName() + " đạt " + formatScore(item.getScore()) + "/5")
                .toList();
    }

    private List<String> buildImprovementAreas(List<SurveyManagerEvaluationReportResponse.DimensionScore> weakDimensions) {
        if (weakDimensions == null || weakDimensions.isEmpty()) {
            return Collections.emptyList();
        }

        return weakDimensions.stream()
                .filter(item -> item.getScore() != null && item.getScore().compareTo(GOOD_DIMENSION_SCORE) < 0)
                .map(item -> item.getDimensionName() + " đạt " + formatScore(item.getScore()) + "/5")
                .toList();
    }

    private List<String> extractOverallComments(List<SurveyManagerEvaluationReportResponse.TextFeedback> textFeedbacks) {
        if (textFeedbacks == null || textFeedbacks.isEmpty()) {
            return Collections.emptyList();
        }

        return textFeedbacks.stream()
                .filter(item -> "OVERALL_COMMENT".equals(upper(item.getDimensionCode())))
                .map(SurveyManagerEvaluationReportResponse.TextFeedback::getAnswer)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String toTextFeedbackCategory(String dimensionCode, String answer) {
        String dimension = normalizeDimension(dimensionCode);
        String text = lower(answer);

        if ("RECOMMENDATION".equals(dimension)) return "RECOMMENDATION";
        if ("OVERALL_COMMENT".equals(dimension)) return "OVERALL_COMMENT";

        if (containsAny(text, "tốt", "mạnh", "giỏi", "nhanh", "chủ động", "phù hợp", "good", "strong")) {
            return "STRENGTH";
        }

        if (containsAny(text, "cần", "thiếu", "yếu", "chưa", "cải thiện", "khó", "need", "improve")) {
            return "IMPROVEMENT";
        }

        return "OTHER";
    }

    private static String normalizeDimension(String value) {
        String raw = upper(value);

        if (!StringUtils.hasText(raw)) {
            return "OVERALL_COMMENT";
        }

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

    private static boolean isScoringDimension(String dimensionCode) {
        String value = normalizeDimension(dimensionCode);

        return switch (value) {
            case "ROLE_FIT",
                 "WORK_QUALITY",
                 "LEARNING_ABILITY",
                 "PROACTIVENESS",
                 "TEAM_INTEGRATION",
                 "ATTITUDE_CULTURE" -> true;
            default -> false;
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

    private static String toDimensionLevel(BigDecimal score) {
        if (score == null || score.compareTo(BigDecimal.ZERO) <= 0) {
            return "NO_SCORE";
        }

        if (score.compareTo(GOOD_DIMENSION_SCORE) >= 0) {
            return "GOOD";
        }

        if (score.compareTo(LOW_DIMENSION_SCORE) >= 0) {
            return "NORMAL";
        }

        if (score.compareTo(BigDecimal.valueOf(2)) >= 0) {
            return "LOW";
        }

        return "RISK";
    }

    private static String toFitLabel(String fitLevel) {
        return switch (upper(fitLevel)) {
            case FIT -> "Phù hợp";
            case FOLLOW_UP -> "Cần theo dõi thêm";
            case NOT_FIT -> "Chưa phù hợp";
            default -> "Chưa được đánh giá";
        };
    }

    private static String toRecommendationLabel(String decision, String rawRecommendation) {
        return switch (upper(decision)) {
            case CONTINUE_OFFICIAL -> "Đề xuất tiếp tục chính thức";
            case EXTEND_PROBATION -> "Đề xuất theo dõi thêm";
            case NEED_TRAINING -> "Đề xuất đào tạo/kèm thêm";
            case NOT_CONTINUE -> "Đề xuất không tiếp tục";
            default -> StringUtils.hasText(rawRecommendation) ? rawRecommendation : "Chưa có đề xuất";
        };
    }

    private String formatDimensionNames(List<SurveyManagerEvaluationReportResponse.DimensionScore> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return "chưa có dữ liệu theo tiêu chí";
        }

        return dimensions.stream()
                .limit(3)
                .map(item -> item.getDimensionName() + " (" + formatScore(item.getScore()) + "/5)")
                .collect(Collectors.joining(", "));
    }

    private String formatScore(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return "—";
        }

        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static BigDecimal avgNullable(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return null;

        if (value instanceof BigDecimal decimal) {
            return decimal;
        }

        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }

        try {
            return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Integer integer(Object value) {
        if (value == null) return null;

        if (value instanceof Integer integer) {
            return integer;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Date date(Object value) {
        if (value == null) return null;
        if (value instanceof Date date) return date;
        if (value instanceof Timestamp timestamp) return new Date(timestamp.getTime());
        return null;
    }

    private List<String> parseChoices(Object valueChoices, Object fallbackText) {
        String raw = firstText(valueChoices, fallbackText);

        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }

        String text = raw.trim();

        if (text.startsWith("[") && text.endsWith("]")) {
            try {
                String[] arr = objectMapper.readValue(text, String[].class);
                List<String> result = new ArrayList<>();

                for (String item : arr) {
                    if (StringUtils.hasText(item)) {
                        result.add(item.trim());
                    }
                }

                return result;
            } catch (Exception ignored) {
                // fallback split below
            }
        }

        return List.of(text.split(",")).stream()
                .map(item -> item.replace("\"", "").trim())
                .filter(StringUtils::hasText)
                .toList();
    }

    private static boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text)) {
            return false;
        }

        for (String keyword : keywords) {
            if (StringUtils.hasText(keyword) && text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }

    private static String firstText(Object... values) {
        for (Object value : values) {
            String text = str(value);
            if (StringUtils.hasText(text)) {
                return text;
            }
        }

        return null;
    }

    private static String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
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

    private static List<Map<String, Object>> safeRows(List<Map<String, Object>> rows) {
        return rows == null ? Collections.emptyList() : rows;
    }
}