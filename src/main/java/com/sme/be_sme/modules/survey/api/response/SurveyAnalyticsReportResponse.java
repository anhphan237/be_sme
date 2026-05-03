package com.sme.be_sme.modules.survey.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyAnalyticsReportResponse {

    private Integer sentCount;
    private Integer submittedCount;
    private BigDecimal responseRate;
    private BigDecimal overallSatisfactionScore;

    private List<DimensionStat> dimensionStats;
    private List<QuestionStat> questionStats;

    private List<DimensionStat> lowScoreDimensions;
    private List<DimensionStat> topPositiveDimensions;

    private List<QuestionStat> lowestQuestions;
    private List<QuestionStat> highestQuestions;

    private List<TrendPoint> timeTrends;
    private List<StageTrend> stageTrends;

    private Integer textResponseCount;
    private Integer ratingQuestionCount;
    private Integer textQuestionCount;
    private Integer choiceQuestionCount;

    private List<ResponseSummary> responseSummaries;

    // NEW: FE ưu tiên dùng field này cho chi tiết theo từng nhân viên
    private List<EmployeeInsight> employeeInsights;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionStat {
        private String dimensionCode;
        private Integer questionCount;
        private Integer responseCount;
        private BigDecimal averageScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionStat {
        private String questionId;
        private String questionText;
        private String questionType;
        private String dimensionCode;
        private Integer responseCount;
        private BigDecimal averageScore;
        private Map<String, Long> choiceDistribution;
        private Integer textAnswerCount;
        private BigDecimal completionRate;
        private List<String> sampleTexts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String bucket;
        private Integer submittedCount;
        private BigDecimal averageScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StageTrend {
        private String stage;
        private Integer submittedCount;
        private BigDecimal averageOverall;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseSummary {
        private String surveyResponseId;
        private String surveyInstanceId;
        private String surveyTemplateId;
        private String templateName;

        private String onboardingId;
        private String stage;

        private String employeeId;
        private String employeeUserId;
        private String employeeName;
        private String employeeEmail;
        private String jobTitle;
        private String departmentName;

        private String managerUserId;
        private String managerName;

        private BigDecimal overallScore;
        private Date submittedAt;

        // NEW
        private List<DimensionScore> dimensionScores;
        private List<AnswerDetail> answerDetails;
        private List<TextFeedback> textFeedbacks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeInsight {
        private String employeeKey;

        private String employeeId;
        private String employeeUserId;
        private String employeeName;
        private String employeeEmail;
        private String jobTitle;
        private String departmentName;

        private String managerUserId;
        private String managerName;

        private Integer responseCount;
        private Integer validScoreCount;

        private BigDecimal averageScore;
        private BigDecimal latestScore;
        private BigDecimal highestScore;
        private BigDecimal lowestScore;
        private BigDecimal scoreSpread;

        private String latestTemplateName;
        private String lowestTemplateName;
        private String highestTemplateName;

        private Date latestSubmittedAt;

        // IMPROVING / DECLINING / STABLE / SINGLE / NO_SCORE
        private String trend;

        // POSITIVE / STABLE / NEED_FOLLOW_UP / RISK / NO_SCORE
        private String riskLevel;

        private List<DimensionScore> dimensionScores;
        private List<DimensionScore> weakDimensions;
        private List<DimensionScore> strongDimensions;

        private List<TextFeedback> textFeedbacks;
        private List<ResponseSummary> responses;

        private String summary;
        private String recommendation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionScore {
        private String dimensionCode;
        private BigDecimal averageScore;
        private Integer answerCount;

        // GOOD / NORMAL / LOW / RISK
        private String level;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDetail {
        private String surveyResponseId;
        private String surveyInstanceId;

        private String questionId;
        private String questionText;
        private String questionType;
        private String dimensionCode;

        private Integer valueRating;
        private String valueText;
        private String valueChoice;
        private List<String> valueChoices;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextFeedback {
        private String surveyResponseId;
        private String surveyInstanceId;
        private String templateName;
        private String questionId;
        private String questionText;
        private String dimensionCode;
        private String answer;
        private Date submittedAt;
    }
}