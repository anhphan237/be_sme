package com.sme.be_sme.modules.survey.api.response;

import lombok.Data;

import java.util.List;
@Data
public class SurveyQuestionListResponse {
    private String templateId;
    private List<SurveyQuestionResponse> questions;
}
