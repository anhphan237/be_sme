package com.sme.be_sme.modules.survey.infrastructure.persistence.entity;

import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

@Data
public class SurveyAiSummaryEntity {

    private String summaryId;
    private String companyId;
    private String templateId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String language;
    private String inputHash;
    private String healthLevel;
    private String summaryJson;
    private Date generatedAt;
    private String generatedBy;
}