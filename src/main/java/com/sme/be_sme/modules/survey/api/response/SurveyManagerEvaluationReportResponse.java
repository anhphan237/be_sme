package com.sme.be_sme.modules.survey.api.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class SurveyManagerEvaluationReportResponse {

    private Summary summary = new Summary();

    private List<EmployeeEvaluationRow> employees = new ArrayList<>();

    @Data
    public static class Summary {
        private int totalEmployees;
        private int sentCount;
        private int submittedCount;
        private int pendingCount;
        private BigDecimal responseRate = BigDecimal.ZERO;
        private BigDecimal averageScore = BigDecimal.ZERO;
        private int fitCount;
        private int needFollowUpCount;
        private int notFitCount;
        private int notEvaluatedCount;
    }

    @Data
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

        private String status;
        private BigDecimal averageScore;
        private String fitLevel;
        private String fitLabel;
        private String recommendation;
        private String recommendationLabel;

        private Date sentAt;
        private Date submittedAt;
        private Date completedAt;

        private List<DimensionScore> dimensionScores = new ArrayList<>();
        private List<TextFeedback> textFeedbacks = new ArrayList<>();
    }

    @Data
    public static class DimensionScore {
        private String dimensionCode;
        private String dimensionName;
        private BigDecimal score = BigDecimal.ZERO;
    }

    @Data
    public static class TextFeedback {
        private String question;
        private String answer;
    }
}
