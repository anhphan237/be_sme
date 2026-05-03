package com.sme.be_sme.modules.survey.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyManagerEvaluationReportResponse {

    private Summary summary;

    private List<EmployeeEvaluationRow> employees;
    private List<EmployeeEvaluationRow> managerEvaluationInsights;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Integer totalEmployees;
        private Integer sentCount;
        private Integer submittedCount;
        private Integer pendingCount;
        private BigDecimal responseRate;
        private BigDecimal averageScore;

        private Integer fitCount;
        private Integer needFollowUpCount;
        private Integer notFitCount;
        private Integer notEvaluatedCount;

        private Integer officialRecommendedCount;
        private Integer trainingRecommendedCount;
        private Integer extendProbationCount;
        private Integer notContinueCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeEvaluationRow {
        private String surveyInstanceId;
        private String surveyResponseId;
        private String onboardingId;

        private String employeeUserId;
        private String employeeName;
        private String employeeEmail;
        private String jobTitle;
        private String departmentName;

        private String managerUserId;
        private String managerName;
        private String managerEmail;

        /**
         * PENDING / SUBMITTED / EXPIRED
         */
        private String status;

        private BigDecimal averageScore;

        /**
         * Raw value từ câu hỏi dimension RECOMMENDATION.
         */
        private String recommendation;

        /**
         * CONTINUE_OFFICIAL / EXTEND_PROBATION / NEED_TRAINING / NOT_CONTINUE / UNKNOWN
         */
        private String recommendationDecision;

        /**
         * Label tiếng Việt cho recommendationDecision.
         */
        private String recommendationLabel;

        /**
         * FIT / FOLLOW_UP / NOT_FIT / NOT_EVALUATED
         */
        private String fitLevel;
        private String fitLabel;

        private Date sentAt;
        private Date submittedAt;
        private Date completedAt;

        private List<DimensionScore> dimensionScores;
        private List<DimensionScore> weakDimensions;
        private List<DimensionScore> strongDimensions;

        private List<TextFeedback> textFeedbacks;
        private List<AnswerDetail> answerDetails;

        /**
         * Nhận định nghiệp vụ sau onboarding.
         */
        private String summary;

        /**
         * Đề xuất hành động cho HR.
         */
        private String actionRecommendation;

        /**
         * Các điểm mạnh / điểm cần cải thiện rút ra từ dimension score.
         */
        private List<String> strengths;
        private List<String> improvementAreas;
        private List<String> overallComments;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionScore {
        private String dimensionCode;
        private String dimensionName;
        private BigDecimal score;
        private Integer answerCount;

        /**
         * GOOD / NORMAL / LOW / RISK / NO_SCORE
         */
        private String level;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextFeedback {
        private String questionId;
        private String question;
        private String answer;
        private String dimensionCode;
        private String dimensionName;

        /**
         * STRENGTH / IMPROVEMENT / OVERALL_COMMENT / RECOMMENDATION / OTHER
         */
        private String category;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDetail {
        private String questionId;
        private String question;
        private String questionType;
        private String dimensionCode;
        private String dimensionName;

        private Integer valueRating;
        private String valueText;
        private String valueChoice;
        private List<String> valueChoices;
    }
}