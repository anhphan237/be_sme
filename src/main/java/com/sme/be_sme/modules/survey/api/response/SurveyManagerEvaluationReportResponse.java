package com.sme.be_sme.modules.survey.api.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class SurveyManagerEvaluationReportResponse {
    private int sentCount;
    private int submittedCount;
    private int pendingCount;
    private BigDecimal responseRate = BigDecimal.ZERO;
    private BigDecimal averageScore = BigDecimal.ZERO;
    private BigDecimal recommendationRate = BigDecimal.ZERO;
    private List<DimensionStat> dimensionStats = new ArrayList<>();
    private List<QuestionStat> questionStats = new ArrayList<>();
    private List<EmployeeEvaluationRow> employees = new ArrayList<>();
    private List<InsightItem> riskItems = new ArrayList<>();
    private List<InsightItem> strengthItems = new ArrayList<>();

    @Data
    public static class DimensionStat {
        private String dimensionCode;
        private int questionCount;
        private int responseCount;
        private BigDecimal averageScore = BigDecimal.ZERO;
    }

    @Data
    public static class QuestionStat {
        private String questionId;
        private String content;
        private String type;
        private String dimensionCode;
        private int responseCount;
        private BigDecimal averageScore;
        private BigDecimal completionRate = BigDecimal.ZERO;
        private int textAnswerCount;
        private Map<String, Integer> choiceDistribution = new LinkedHashMap<>();
        private List<String> sampleTexts = new ArrayList<>();
    }

    @Data
    public static class EmployeeEvaluationRow {
        private String surveyInstanceId;
        private String surveyResponseId;
        private String onboardingId;
        private String templateId;
        private String templateName;
        private String employeeUserId;
        private String employeeName;
        private String employeeEmail;
        private String managerUserId;
        private String managerName;
        private String status;
        private BigDecimal overallScore;
        private String recommendation;
        private Date sentAt;
        private Date submittedAt;
        private Date completedAt;
    }

    @Data
    public static class InsightItem {
        private String label;
        private BigDecimal value = BigDecimal.ZERO;
        private String subtext;
    }
}
