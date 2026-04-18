package com.sme.be_sme.modules.analytics.api.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyOnboardingTemplateScoreboardResponse {
    private String companyId;
    private String templateId;
    private String status;
    private Integer totalCandidates;
    private List<CandidateScoreItem> candidates;

    @Getter
    @Setter
    public static class CandidateScoreItem {
        private Integer rank;
        private String instanceId;
        private String employeeId;
        private Integer progressPercent;
        private Integer totalTasks;
        private Integer completedTasks;
        private Integer overdueTasks;
        private Integer lateCompletedTasks;
        private Double completionRate;
        private Double qualityScore;
    }
}
