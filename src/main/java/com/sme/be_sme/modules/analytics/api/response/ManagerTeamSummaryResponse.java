package com.sme.be_sme.modules.analytics.api.response;

import lombok.Data;

import java.util.List;

@Data
public class ManagerTeamSummaryResponse {
    private String companyId;
    private String managerUserId;

    private Integer teamMemberCount;
    private Integer totalOnboarding;
    private Integer activeOnboarding;
    private Integer completedOnboarding;
    private Integer cancelledOnboarding;
    private Double averageProgress;

    private Integer attentionCount;
    private List<AttentionEmployee> attentionEmployees;

    @Data
    public static class AttentionEmployee {
        private String onboardingId;
        private String employeeId;
        private String status;
        private Integer progressPercent;
        private String reason;
    }
}