package com.sme.be_sme.modules.analytics.api.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class CandidateFitAssessResponse {
    private String auditId;
    private String employeeId;
    private String fitType;
    private String fitLevel;
    private Double fitScore;
    private WeightConfig weights;
    private Date assessedAt;

    @Getter
    @Setter
    public static class WeightConfig {
        private Double jdWeight;
        private Double competencyWeight;
        private Double interviewWeight;
    }
}

