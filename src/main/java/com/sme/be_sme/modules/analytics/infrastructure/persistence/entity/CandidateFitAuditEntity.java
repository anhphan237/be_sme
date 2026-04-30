package com.sme.be_sme.modules.analytics.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class CandidateFitAuditEntity {
    private String candidateFitAuditId;
    private String companyId;
    private String employeeId;
    private Double jdMatchScore;
    private Double competencyScore;
    private Double interviewScore;
    private Double jdWeight;
    private Double competencyWeight;
    private Double interviewWeight;
    private Double fitScore;
    private String fitLevel;
    private String note;
    private String assessedBy;
    private Date assessedAt;
    private Date createdAt;
}

