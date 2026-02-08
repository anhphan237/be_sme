package com.sme.be_sme.modules.survey.api.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SurveySatisfactionReportResponse {
    /** Overall satisfaction: average of response overall_score when present, else null */
    private BigDecimal overallSatisfactionScore;
    /** Aggregation per question */
    private List<QuestionStat> questionStats;

    @Getter
    @Setter
    public static class QuestionStat {
        private String questionId;
        private String questionText;
        private String questionType;
        /** For RATING type: average of value_rating */
        private BigDecimal averageScore;
        /** For CHOICE type: value_choice -> count */
        private Map<String, Long> choiceDistribution;
        /** Number of responses that answered this question */
        private int responseCount;
    }
}
