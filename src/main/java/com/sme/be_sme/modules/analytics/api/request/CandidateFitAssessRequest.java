package com.sme.be_sme.modules.analytics.api.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CandidateFitAssessRequest {
    private String employeeId;
    private Double jdMatchScore;
    private Double competencyScore;
    private Double interviewScore;
    /** Optional; default 0.4 */
    private Double jdWeight;
    /** Optional; default 0.35 */
    private Double competencyWeight;
    /** Optional; default 0.25 */
    private Double interviewWeight;
    /** Optional note for evaluator context/audit. */
    private String note;
}

