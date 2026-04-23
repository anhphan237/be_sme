package com.sme.be_sme.modules.survey.api.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class SurveyAiSummaryResponse {

    private String healthLevel;
    private String summary;
    private List<String> keyFindings = new ArrayList<>();
    private List<String> recommendedActions = new ArrayList<>();
    private String riskExplanation;
    private String positiveSignal;
    private Boolean fromCache;
    private Date generatedAt;

    private String source;
    private Boolean aiAvailable;
    private String errorMessage;
}