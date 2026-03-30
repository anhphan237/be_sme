package com.sme.be_sme.modules.survey.api.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SurveyAnalyticsReportResponse {


    private Integer sentCount;        // số survey instance đã gửi (nếu có)
    private Integer submittedCount;   // số survey response submit
    private BigDecimal responseRate;  // submitted/sent (0-1)

    // 2) overall
    private BigDecimal overallSatisfactionScore;

    // 3) breakdown
    private List<DimensionStat> dimensionStats;
    private List<QuestionStat> questionStats;

    private List<DimensionStat> lowScoreDimensions;
    private List<DimensionStat> topPositiveDimensions;
    // 4) trend (option)
    private List<StageTrend> stageTrends;
    private List<TrendPoint> timeTrends;

    private Integer textResponseCount;
    private Integer ratingQuestionCount;
    private Integer textQuestionCount;
    private Integer choiceQuestionCount;

    private List<QuestionStat> lowestQuestions;
    private List<QuestionStat> highestQuestions;
    private List<ResponseSummary> responseSummaries;
    @Getter @Setter
    public static class DimensionStat {
        private String dimensionCode;     // e.g. IT_SETUP / HR_SUPPORT
        private Integer questionCount;    // số câu measurable trong dimension
        private Integer responseCount;    // tổng answer count
        private BigDecimal averageScore;  // avg rating
    }

    @Getter @Setter
    public static class QuestionStat {
        private String questionId;
        private String questionText;
        private String questionType;      // RATING/CHOICE/TEXT
        private String dimensionCode;
        private Integer responseCount;

        // rating
        private BigDecimal averageScore;

        // choice
        private Map<String, Long> choiceDistribution;

        // text (simple)
        private Integer textAnswerCount;

        private BigDecimal completionRate;
        private List<String> sampleTexts;
    }

    @Getter @Setter
    public static class StageTrend {
        private String stage;                 // D7/D30...
        private Integer submittedCount;
        private BigDecimal averageOverall;    // avg overall_score theo stage
    }

    @Getter @Setter
    public static class TrendPoint {
        private String bucket;
        private Integer submittedCount;
        private BigDecimal averageScore;
    }
    @Getter
    @Setter
    public static class ResponseSummary {
        private String surveyResponseId;
        private String surveyInstanceId;
        private String templateName;
        private String employeeName;
        private BigDecimal overallScore;
        private Date submittedAt;
    }
}