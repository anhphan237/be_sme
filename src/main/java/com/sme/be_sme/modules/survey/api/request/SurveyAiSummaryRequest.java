package com.sme.be_sme.modules.survey.api.request;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SurveyAiSummaryRequest {

    private String templateId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String language;
    private Boolean forceRefresh;

    private JsonNode analyticsSnapshot;
}